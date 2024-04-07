package eclipse.plugin.aiassistant.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.IndentationFormatter;

/**
 * This class represents a JavaScript function that replaces the selected code
 * block in the active text editor.
 */
public class ReplaceSelectionBrowserFunction extends DisableableBrowserFunction {
	
	/**
	 * Constructs a new instance of the ReplaceSelectionFunction class.
	 *
	 * @param browser The browser in which this function is used.
	 * @param name    The name of this function.
	 */
	public ReplaceSelectionBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * This method is called when the JavaScript function is invoked. It replaces
	 * the selected code block in the active text editor with the provided new code
	 * block.
	 *
	 * @param arguments The arguments passed to this function. The first argument
	 *                  should be a string containing the new code block to replace
	 *                  the selected code block with.
	 * @return Always returns null.
	 */
	@Override
	public Object function(Object[] arguments) {
		if (isEnabled() && arguments.length > 0 && arguments[0] instanceof String) {
			String newCodeBlock = (String) arguments[0];
			if (!newCodeBlock.isEmpty()) {
				ITextEditor textEditor = Eclipse.getActiveTextEditor();
				if (textEditor != null) {
					// Save all dirty editors for Eclipse.getSelectedTextOrEditorText().
					Eclipse.saveAllEditors(false);
					String oldCodeBlock = Eclipse.getSelectedTextOrEditorText(textEditor);
					if (!oldCodeBlock.isEmpty()) {
						newCodeBlock = IndentationFormatter.matchIndentation(oldCodeBlock, newCodeBlock);
						Eclipse.replaceAndSelectText(textEditor, newCodeBlock);
					}
				}
			}
		}
		return null;
	}
	
}