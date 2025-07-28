package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class handles the visibility of the "Fix Warnings" option in the right click context menu.
 * The "Fix Warnings" option is only visible when there are warnings from the compiler in the active editor.
 */
public class FixWarningsVisibilityHandler extends PropertyTester {

	// The unique identifier for this property tester
	static final String CONDITION_TESTER = "FixWarningsVisibilityHandler";

	/**
	 * This method is called by Eclipse to test the visibility of the "Fix Warnings" option in the right click context menu.
	 * It checks if there are any compiler warnings in the active editor and returns true if there are, false otherwise.
	 *
	 * @param receiver The object that the property tester is being applied to.
	 * @param property The name of the property being tested.
	 * @param args Any arguments passed to the property tester.
	 * @param expectedValue The expected value of the property.
	 * @return True if there are compiler warnings in the active editor, false otherwise.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		// https://stackoverflow.com/questions/76835129/remove-context-menu-items-of-eclipse-plugins
		if (CONDITION_TESTER.equals(property)) {
			ITextEditor textEditor = Eclipse.getActiveTextEditor();
			if (textEditor != null) {
				IFile activeFile = Eclipse.getActiveFile(textEditor);
				if (activeFile != null) {
					return Eclipse.getCompilerErrors(activeFile).isEmpty() &&
							!Eclipse.getCompilerWarnings(activeFile).isEmpty();
				}
			}
			return false;
		}
		return false;
	}

}