package eclipse.plugin.aiassistant.browser;

import org.eclipse.swt.browser.Browser;

import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class represents a JavaScript function that copies code to the
 * clipboard.
 */
public class CopyCodeBrowserFunction extends DisableableBrowserFunction {
	
	/**
	 * Constructs a new instance of the CopyCodeFunction class.
	 *
	 * @param browser The browser in which this function is used.
	 * @param name    The name of this function.
	 */
	public CopyCodeBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * This method is called when the JavaScript function is invoked. It copies the
	 * provided code block to the clipboard.
	 *
	 * @param arguments The arguments passed to this function. The first argument
	 *                  should be a string containing the code block to copy.
	 * @return Always returns null.
	 */
	@Override
	public Object function(Object[] arguments) {
		if (isEnabled() && arguments.length > 0 && arguments[0] instanceof String) {
			String codeBlock = (String) arguments[0];
			Eclipse.setClipboardContents(codeBlock);
		}
		return null;
	}
	
}