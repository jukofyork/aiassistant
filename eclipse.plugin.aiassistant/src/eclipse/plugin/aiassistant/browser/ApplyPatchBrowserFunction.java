package eclipse.plugin.aiassistant.browser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class represents a JavaScript function that applies a patch to the
 * active text editor.
 */
public class ApplyPatchBrowserFunction extends DisableableBrowserFunction {

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
			ITextEditor textEditor = Eclipse.getActiveTextEditor();
			if (textEditor != null) {
				try (var patchInputStream = new ByteArrayInputStream(patchString.getBytes(StandardCharsets.UTF_8))) {
					var patchStorage = new PatchStorage(patchString);
					IWorkbenchPart activeWorkbenchPart = Eclipse.getActivePart();
					IFile targetFile = Eclipse.getActiveFile(textEditor);
					ApplyPatchOperation operation = new ApplyPatchOperation(activeWorkbenchPart, patchStorage,
							targetFile, new CompareConfiguration());
					operation.openWizard();
				} catch (Exception e) {
					Logger.error(e.getLocalizedMessage(), e);
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