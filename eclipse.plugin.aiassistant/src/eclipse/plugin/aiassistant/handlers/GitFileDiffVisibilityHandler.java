package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.expressions.PropertyTester;

import eclipse.plugin.aiassistant.utility.GitDiff;

/**
 * This class is responsible for handling the visibility of current file diff menu items in the Eclipse plugin.
 * It extends the PropertyTester class provided by Eclipse and overrides the test method.
 * The test method checks if there are working directory changes for the current file in the Git repository and returns a boolean value accordingly.
 */
public class GitFileDiffVisibilityHandler extends PropertyTester {

	// The unique identifier for this property tester
	static final String CONDITION_TESTER = "GitFileDiffVisibilityHandler";

	/**
	 * This method is called by Eclipse to test the visibility of current file diff menu items.
	 * It checks if there are working directory changes for the current file in the Git repository and returns true if there are, false otherwise.
	 *
	 * @param receiver The object that the property tester is being applied to.
	 * @param property The name of the property being tested.
	 * @param args Any arguments passed to the property tester.
	 * @param expectedValue The expected value of the property.
	 * @return True if there are working directory changes for the current file in the Git repository, false otherwise.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		try {
			return GitDiff.isRepositoryAvailable() && !GitDiff.getCurrentFileStagedDiff().isEmpty();
		} catch (Exception e) {
			return false;
		}
	}
}