package eclipse.plugin.aiassistant.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

/**
 * A custom implementation of BrowserFunction that allows blocking of the
 * Javascript call-back. This class extends the BrowserFunction provided by the
 * SWT library and adds an enable/disable functionality.
 */
public abstract class DisableableBrowserFunction extends BrowserFunction {

	private boolean isEnabled = true;

	/**
	 * Constructs a new instance of DisableableBrowserFunction with the specified
	 * browser and name.
	 *
	 * @param browser The browser instance that this function belongs to.
	 * @param name    The name of the function as it appears in the Javascript
	 *                environment.
	 */
	public DisableableBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * Checks if the function is currently enabled.
	 *
	 * @return True if the function is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Enables or disables the function based on the provided boolean value.
	 *
	 * @param enabled True to enable the function, false to disable it.
	 */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}
}