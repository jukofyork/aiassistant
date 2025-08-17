package eclipse.plugin.aiassistant.browser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class represents a JavaScript function that applies a patch to the active text editor.
 *
 * For helpful guides on using Unified diffs with LLMs see:
 * - https://aider.chat/docs/unified-diffs.html#choose-a-familiar-editing-format
 * - https://github.com/Aider-AI/aider/blob/main/aider/coders/udiff_prompts.py
 */
public class ApplyPatchBrowserFunction extends DisableableBrowserFunction {

	private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("@@\\s*-\\d+(?:,\\d+)?\\s*\\+\\d+(?:,\\d+)?\\s*@@");

	/**
	 * Constructs a new instance of the ApplyPatchFunction class.
	 *
	 * @param browser The browser in which this function is used.
	 * @param name    The name of this function.
	 */
	public ApplyPatchBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * This method is called when the JavaScript function is invoked. It applies a
	 * patch to the active text editor using the provided code block.
	 *
	 * @param arguments The arguments passed to this function. The first argument
	 *                  should be a string containing the code block to apply as a
	 *                  patch.
	 * @return Always returns null.
	 */
	@Override
	public Object function(Object[] arguments) {
		if (isEnabled() && arguments.length > 0 && arguments[0] instanceof String) {
			String patchString = (String) arguments[0];
			patchString = removePrefixes(patchString);
			patchString = normalizeHunkHeaders(patchString);

			ITextEditor textEditor = Eclipse.getActiveTextEditor();
			if (textEditor != null) {
				IWorkbenchPart part = Eclipse.getActivePart();
				IProject target = Eclipse.getActiveProject(textEditor);
				if (target != null) {
					var patchStorage = new PatchStorage(patchString);
					CompareConfiguration config = new CompareConfiguration();
					config.setProperty("IGNORE_WHITESPACE", Boolean.TRUE);
					ApplyPatchOperation operation = new ApplyPatchOperation(part, patchStorage, target, config);
					operation.openWizard();
				}
			}
		}
		return null;
	}

	/**
	 * This method removes the prefixes from the patch string if they exist.
	 *
	 * @param patchString The patch string to be processed.
	 * @return The processed patch string.
	 */
	private String removePrefixes(String patchString) {
		return patchString.replace("--- a/", "--- /").replace("+++ b/", "+++ /");
	}

	/**
	 * This method converts hunk headers with line numbers to the generic @@ ... @@ format.
	 *
	 * @param patchString The patch string to be processed.
	 * @return The processed patch string with normalized hunk headers.
	 */
	private String normalizeHunkHeaders(String patchString) {
		return HUNK_HEADER_PATTERN.matcher(patchString).replaceAll("@@ ... @@");
	}

	/**
	 * The PatchStorage class provides a way to store and retrieve patch content.
	 */
	private static class PatchStorage implements IStorage {
		private final String patch;

		public PatchStorage(String patch) {
			this.patch = patch;
		}

		@Override
		public <T> T getAdapter(Class<T> arg0) {
			return null;
		}

		@Override
		public InputStream getContents() throws CoreException {
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