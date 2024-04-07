package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.expressions.PropertyTester;

import eclipse.plugin.aiassistant.view.MainView;

/**
 * This class is responsible for handling the visibility of the menu items in the Eclipse plugin.
 * It extends the PropertyTester class provided by Eclipse and overrides the test method.
 * The test method checks if the main view of the AI assistant is present and returns a boolean value accordingly.
 */
public class MenuVisibilityHandler extends PropertyTester {

    // The unique identifier for this property tester
	static final String CONDITION_TESTER = "MenuVisibilityHandler";
	
    /**
     * This method is called by Eclipse to test the visibility of the menu items.
     * It checks if the main view of the AI assistant is present and returns true if it is, false otherwise.
     *
     * @param receiver The object that the property tester is being applied to.
     * @param property The name of the property being tested.
     * @param args Any arguments passed to the property tester.
     * @param expectedValue The expected value of the property.
     * @return True if the main view of the AI assistant is present, false otherwise.
     */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		// https://stackoverflow.com/questions/76835129/remove-context-menu-items-of-eclipse-plugins
		// https://stackoverflow.com/questions/22374204/add-context-menu-entry-to-texteditor (see #AbstractTextEditorContext)
		return MainView.findMainView().isPresent();
	}

}