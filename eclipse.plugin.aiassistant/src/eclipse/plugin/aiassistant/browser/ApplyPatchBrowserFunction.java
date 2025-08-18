package eclipse.plugin.aiassistant.browser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch2;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.patch.PatchParser;
import org.eclipse.compare.patch.ReaderCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * Browser function that applies unified diff patches from the AI assistant interface
 * to files in the Eclipse workspace. This function integrates with Eclipse's patch
 * infrastructure to validate, filter, and apply patches while handling common format
 * issues and rejection scenarios.
 *
 * <p>The function performs intelligent patch processing by:
 * <ul>
 * <li>Normalizing patch format (EOL handling, path prefixes, hunk lengths)</li>
 * <li>Attempting application with multiple strip levels to find the best fit</li>
 * <li>Filtering out rejected hunks and providing detailed feedback</li>
 * <li>Opening Eclipse's patch application wizard for user review</li>
 * </ul>
 *
 * @see DisableableBrowserFunction
 * @see org.eclipse.compare.patch.ApplyPatchOperation
 */
public class ApplyPatchBrowserFunction extends DisableableBrowserFunction {

	/**
	 * Regex pattern for parsing unified diff hunk headers.
	 * Matches format: @@ -oldStart[,oldLen] +newStart[,newLen] @@ trailing
	 */
	private static final Pattern HUNK_HEADER = Pattern.compile("^@@\\s*-(\\d+)(?:,\\d+)?\\s*\\+(\\d+)(?:,\\d+)?\\s*@@(.*)$");

	/**
	 * Creates a new patch application browser function.
	 *
	 * @param browser the SWT browser instance
	 * @param name the JavaScript function name to register
	 */
	public ApplyPatchBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * Applies a patch to the currently active file in Eclipse. This method validates
	 * the patch format, attempts application with different configurations, handles
	 * rejections intelligently, and opens the Eclipse patch wizard for final review.
	 *
	 * <p>The process runs asynchronously to avoid blocking the UI thread and performs
	 * dry-run validation before opening the wizard to provide early feedback on issues.
	 *
	 * @param arguments array containing the patch text as the first element
	 * @return null (browser functions don't return meaningful values)
	 */
	@Override
	public Object function(Object[] arguments) {
		if (!isEnabled() || arguments.length == 0 || !(arguments[0] instanceof String)) {
			return null;
		}

		ITextEditor editor = Eclipse.getActiveTextEditor();
		if (editor == null) return null;

		IProject project = Eclipse.getActiveProject(editor);
		IFile file = Eclipse.getActiveFile(editor);
		if (project == null || file == null) {
			Logger.warning("Apply Patch requires an active editor with a file in a project.");
			return null;
		}

		final String inputPatch = (String) arguments[0];
		final IWorkbenchPart part = Eclipse.getActivePart();

		Job.create("Apply Patch", monitor -> {
			try {
				String fixed = fixPatch(inputPatch);

				List<Span> spans = new ArrayList<>();
				List<String> hunkTexts = extractHunks(fixed, spans);

				// Try both strip=0 and strip=1 to find the best application strategy
				Attempt a0 = dryRunAttempt(fixed, file, 0);
				Attempt a1 = dryRunAttempt(fixed, file, 1);
				Attempt best = pickBest(a0, a1);

				Display.getDefault().asyncExec(() -> {
					if (best == null || !best.parseOk) {
						Logger.warning("Patch could not be analyzed; ensure it is a single-file unified diff.");
						return;
					}
					if (best.totalHunks == 0) {
						Logger.warning("The patch contains no hunks to apply.");
						return;
					}

					if (best.rejectedIndices.isEmpty()) {
						// Clean application - open wizard directly
						openWizard(part, project, fixed);
						return;
					}

					// Handle partial rejections by filtering and providing feedback
					String report = buildRejectReport(best, hunkTexts);
					String filtered = filterOutRejectedHunks(fixed, spans, best.rejectedIndices);

					if (filtered == null || filtered.trim().isEmpty() || extractHunks(filtered, null).isEmpty()) {
						Logger.warning(report);
					} else {
						Logger.warning(report);
						openWizard(part, project, filtered);
					}
				});
			} catch (Exception e) {
				Logger.warning("Unexpected error during patch processing", e);
			}
			return Status.OK_STATUS;
		}).schedule();

		return null;
	}

	/**
	 * Opens the Eclipse patch application wizard with the given patch content.
	 *
	 * @param part the workbench part to associate with the operation
	 * @param project the target project for patch application
	 * @param patchText the normalized patch content
	 */
	private void openWizard(IWorkbenchPart part, IProject project, String patchText) {
		CompareConfiguration config = new CompareConfiguration();
		config.setProperty("IGNORE_WHITESPACE", Boolean.TRUE);
		new ApplyPatchOperation(part, new PatchStorage(patchText), project, config).openWizard();
	}

	// ---- Patch format normalization and fixing ----

	/**
	 * Normalizes patch format to ensure compatibility with Eclipse's patch parser.
	 * Fixes common issues including EOL inconsistencies, Git path prefixes, and
	 * incorrect hunk length calculations.
	 *
	 * @param patch the raw patch text
	 * @return normalized patch text ready for parsing
	 */
	private String fixPatch(String patch) {
		if (patch == null) patch = "";
		// Normalize line endings to Unix format
		patch = patch.replace("\r\n", "\n").replace("\r", "\n");
		// Remove Git-style a/ and b/ path prefixes that can confuse Eclipse
		patch = patch.replace("--- a/", "--- ").replace("+++ b/", "+++ ");
		return recalcHunkLengths(patch);
	}

	/**
	 * Recalculates hunk lengths in patch headers to ensure accuracy. Many AI-generated
	 * patches have incorrect length values in @@ headers, which causes parse failures.
	 * This method scans each hunk's actual content to compute correct old/new lengths.
	 *
	 * @param patch the input patch with potentially incorrect hunk lengths
	 * @return patch with corrected hunk header lengths
	 */
	private String recalcHunkLengths(String patch) {
		String[] lines = patch.split("\n", -1);
		List<String> out = new ArrayList<>(lines.length);
		for (int i = 0; i < lines.length; ) {
			String line = lines[i];
			Matcher m = HUNK_HEADER.matcher(line);
			if (!m.matches()) { out.add(line); i++; continue; }

			String oldStart = m.group(1);
			String newStart = m.group(2);
			String trailing = m.group(3) == null ? "" : m.group(3);

			// Find the end of this hunk (next @@ or end of file)
			int j = i + 1;
			while (j < lines.length && !lines[j].startsWith("@@")) j++;

			// Count actual additions/deletions/context lines
			int oldLen = 0, newLen = 0;
			for (int k = i + 1; k < j; k++) {
				if (lines[k].startsWith("-")) oldLen++;
				else if (lines[k].startsWith("+")) newLen++;
				else if (lines[k].startsWith(" ")) { oldLen++; newLen++; }
				// Ignore metadata lines like "\ No newline at end of file"
			}

			out.add(String.format("@@ -%s,%d +%s,%d @@%s", oldStart, oldLen, newStart, newLen, trailing));
			for (int k = i + 1; k < j; k++) out.add(lines[k]);
			i = j;
		}
		return String.join("\n", out);
	}

	// ---- Dry run validation and strategy selection ----

	/**
	 * Represents the result of a patch application attempt, tracking success rate
	 * and configuration details for comparison between different strategies.
	 */
	private static final class Attempt {
		/** Whether the patch could be successfully parsed */
		final boolean parseOk;
		/** The path strip level used (0 = no stripping, 1 = remove one path component) */
		final int strip;
		/** Total number of hunks in the patch */
		final int totalHunks;
		/** Zero-based indices of hunks that were rejected during application */
		final Set<Integer> rejectedIndices;

		Attempt(boolean ok, int s, int total, Set<Integer> rej) {
			parseOk = ok; strip = s; totalHunks = total; rejectedIndices = rej;
		}
	}

	/**
	 * Performs a dry-run patch application to assess viability without modifying files.
	 * This allows testing different strip levels and identifying rejected hunks before
	 * presenting options to the user.
	 *
	 * @param patch the normalized patch content
	 * @param targetFile the file to apply the patch against
	 * @param strip the number of path components to strip from patch paths
	 * @return attempt result with success metrics and rejection details
	 */
	private Attempt dryRunAttempt(String patch, IFile targetFile, int strip) {
		try {
			IFilePatch2[] fps = PatchParser.parsePatch(new StringReaderCreator(patch));
			if (fps == null || fps.length != 1) {
				return new Attempt(false, strip, 0, Collections.emptySet());
			}
			IFilePatch2 fp = fps[0];
			IHunk[] allHunks = fp.getHunks();
			if (allHunks == null) allHunks = new IHunk[0];

			PatchConfiguration cfg = new PatchConfiguration();
			cfg.setIgnoreWhitespace(true);
			cfg.setPrefixSegmentStripCount(strip);

			IFilePatchResult res = fp.apply(new FileContentsReaderCreator(targetFile), cfg, new NullProgressMonitor());
			if (res == null) return new Attempt(false, strip, 0, Collections.emptySet());

			// Identify which specific hunks were rejected
			IHunk[] rejected = res.hasRejects() ? res.getRejects() : new IHunk[0];
			Set<IHunk> rejSet = new HashSet<>(Arrays.asList(rejected));

			Set<Integer> idxs = new HashSet<>();
			for (int i = 0; i < allHunks.length; i++) {
				if (rejSet.contains(allHunks[i])) idxs.add(i);
			}
			return new Attempt(true, strip, allHunks.length, idxs);
		} catch (CoreException e) {
			Logger.warning("Error during patch dry run (strip=" + strip + ")", e);
			return new Attempt(false, strip, 0, Collections.emptySet());
		}
	}

	/**
	 * Selects the better of two patch application attempts based on success rate
	 * and configuration preferences. Prioritizes fewer rejections, then lower strip levels.
	 *
	 * @param a first attempt result
	 * @param b second attempt result
	 * @return the better attempt, or null if both failed
	 */
	private Attempt pickBest(Attempt a, Attempt b) {
		if (a != null && a.parseOk && (b == null || !b.parseOk)) return a;
		if (b != null && b.parseOk && (a == null || !a.parseOk)) return b;
		if (a == null || b == null || !a.parseOk || !b.parseOk) return null;

		int ar = a.rejectedIndices.size(), br = b.rejectedIndices.size();
		if (ar != br) return ar < br ? a : b;
		// Prefer lower strip levels when rejection counts are equal
		return a.strip <= b.strip ? a : b;
	}

	// ---- Hunk extraction, filtering, and reporting ----

	/**
	 * Represents the line span of a hunk within the patch text for filtering operations.
	 */
	private static final class Span {
		/** Starting line index (inclusive) */
		final int start;
		/** Ending line index (exclusive) */
		final int end;
		Span(int s,int e){start=s;end=e;}
	}

	/**
	 * Extracts individual hunk text blocks from a patch and optionally records their
	 * line spans for later filtering operations.
	 *
	 * @param patch the complete patch text
	 * @param spansOut optional list to populate with hunk line spans
	 * @return list of individual hunk text blocks
	 */
	private List<String> extractHunks(String patch, List<Span> spansOut) {
		String[] lines = patch.split("\n", -1);
		List<String> texts = new ArrayList<>();
		for (int i = 0; i < lines.length; ) {
			if (!lines[i].startsWith("@@")) { i++; continue; }
			int start = i++;
			while (i < lines.length && !lines[i].startsWith("@@")) i++;
			int end = i;

			if (spansOut != null) spansOut.add(new Span(start, end));
			StringBuilder sb = new StringBuilder();
			for (int k = start; k < end; k++) {
				if (k > start) sb.append('\n');
				sb.append(lines[k]);
			}
			texts.add(sb.toString());
		}
		return texts;
	}

	/**
	 * Creates a filtered patch with rejected hunks removed, preserving patch headers
	 * and other non-hunk content. This allows partial application when some hunks fail.
	 *
	 * @param patch the original patch text
	 * @param spans line spans of each hunk in the patch
	 * @param rejectedIdx zero-based indices of hunks to filter out
	 * @return filtered patch text with rejected hunks removed
	 */
	private String filterOutRejectedHunks(String patch, List<Span> spans, Set<Integer> rejectedIdx) {
		String[] lines = patch.split("\n", -1);
		StringBuilder out = new StringBuilder(patch.length());
		int cursor = 0;

		// Copy any header content before first hunk
		int firstStart = spans.isEmpty() ? lines.length : spans.get(0).start;
		for (; cursor < firstStart; cursor++) out.append(lines[cursor]).append('\n');

		// Process each hunk, including only non-rejected ones
		for (int h = 0; h < spans.size(); h++) {
			Span s = spans.get(h);
			if (!rejectedIdx.contains(h)) {
				for (int i = s.start; i < s.end; i++) out.append(lines[i]).append('\n');
			}
			cursor = s.end;
		}

		// Copy any trailing content after last hunk
		for (; cursor < lines.length; cursor++) out.append(lines[cursor]).append('\n');

		String result = out.toString();
		return result.endsWith("\n") ? result.substring(0, result.length() - 1) : result;
	}

	/**
	 * Builds a detailed report of rejected hunks for user feedback, including
	 * the specific hunk content that failed to apply.
	 *
	 * @param attempt the patch attempt with rejection information
	 * @param hunkTexts the text content of each hunk
	 * @return formatted report describing rejected hunks
	 */
	private String buildRejectReport(Attempt attempt, List<String> hunkTexts) {
		List<Integer> idxs = new ArrayList<>(attempt.rejectedIndices);
		idxs.sort(Integer::compareTo);
		StringBuilder sb = new StringBuilder();
		sb.append("The patch could not be applied cleanly (strip=").append(attempt.strip).append("). ");
		sb.append("Rejected ").append(idxs.size()).append(" of ").append(attempt.totalHunks).append(" hunks:\n\n");
		for (int idx : idxs) {
			if (idx >= 0 && idx < hunkTexts.size()) {
				sb.append("```\n").append(hunkTexts.get(idx).trim()).append("\n```\n\n");
			}
		}
		return sb.toString().trim();
	}

	// ---- Reader and IStorage helper implementations ----

	/**
	 * ReaderCreator implementation that provides access to string content for patch parsing.
	 */
	private static class StringReaderCreator extends ReaderCreator {
		private final String content;

		StringReaderCreator(String content) {
			this.content = content == null ? "" : content;
		}

		@Override
		public Reader createReader() {
			return new StringReader(content);
		}
	}

	/**
	 * ReaderCreator implementation that provides access to Eclipse file content with
	 * proper charset handling for patch operations.
	 */
	private static class FileContentsReaderCreator extends ReaderCreator {
		private final IFile file;

		FileContentsReaderCreator(IFile file) {
			this.file = file;
		}

		@Override
		public Reader createReader() throws CoreException {
			try {
				InputStream is = file.getContents(true);
				String charset = file.getCharset(true);
				return new InputStreamReader(is, Charset.forName(charset));
			} catch (Exception e) {
				throw new CoreException(Status.error("Failed to read file content", e));
			}
		}
	}

	/**
	 * IStorage implementation for patch content that allows Eclipse's patch infrastructure
	 * to access the patch text as if it were a workspace resource.
	 */
	private static class PatchStorage implements IStorage {
		private final String patch;

		PatchStorage(String patch) {
			this.patch = patch == null ? "" : patch;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(patch.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public IPath getFullPath() {
			return null;
		}

		@Override
		public String getName() {
			return "patch";
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}
	}
}