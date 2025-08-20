package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.expressions.PropertyTester;

import eclipse.plugin.aiassistant.view.MainView;

/**
 * Property tester that controls visibility of menu items based on MainView availability.
 *
 * References:
 * - https://stackoverflow.com/questions/76835129/remove-context-menu-items-of-eclipse-plugins
 * - https://stackoverflow.com/questions/22374204/add-context-menu-entry-to-texteditor
 */
public class MenuVisibilityHandler extends PropertyTester {

	/** Property tester identifier */
	static final String CONDITION_TESTER = "MenuVisibilityHandler";

	/**
	 * Tests if menu items should be visible.
	 *
	 * @return true if MainView is present
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return MainView.findMainView().isPresent();
	}

}