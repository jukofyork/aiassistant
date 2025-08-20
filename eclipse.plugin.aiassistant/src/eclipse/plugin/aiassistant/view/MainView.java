package eclipse.plugin.aiassistant.view;

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * MainView is responsible for displaying the AI assistant's chat interface
 * within the Eclipse IDE. It manages user interactions and integrates different
 * UI components such as the chat area, input area, and button bar.
 */
public class MainView extends ViewPart {

	/** The unique identifier for this view within the Eclipse plugin framework. */
	public static final String ID = "eclipse.plugin.aiassistant.view.MainView";

	private MainPresenter mainPresenter;

	private SashForm sashForm;
	private Composite mainContainer;
	private ChatConversationArea chatMessageArea;
	private UserInputArea userInputArea;
	private ButtonBarArea buttonBarArea;

	/**
	 * Initializes the view components and sets up the presenter.
	 *
	 * @param parent The parent composite on which this view is built.
	 */
	@Override
	public void createPartControl(Composite parent) {
		mainPresenter = new MainPresenter(this);
		sashForm = new SashForm(parent, SWT.VERTICAL);
		mainContainer = createMainContainer(sashForm);
		chatMessageArea = new ChatConversationArea(mainPresenter, mainContainer);
		userInputArea = new UserInputArea(mainPresenter, mainContainer);
		buttonBarArea = new ButtonBarArea(mainPresenter, mainContainer);
		setInputEnabled(true); // Will turn off stop and set everything else on.
		mainPresenter.loadStateFromPreferenceStore(); // Runs asynchronously on UI thread.
	}

	/**
	 * Saves the current state when the view is disposed.
	 */
	@Override
	public void dispose() {
		super.dispose();
		mainPresenter.saveStateToPreferenceStore(); // Runs synchronously on UI thread.
	}

	/**
	 * Provides access to the MainPresenter which handles the logic for user
	 * interactions.
	 *
	 * @return The MainPresenter instance managing this view.
	 */
	public MainPresenter getMainPresenter() {
		return mainPresenter;
	}

	/**
	 * Sets the focus to the user input area when the view gains focus.
	 */
	@Override
	public void setFocus() {
		Eclipse.runOnUIThreadAsync(() -> userInputArea.setFocus());
	}

	/**
	 * Checks if the main SashForm component is disposed.
	 *
	 * @return true if the SashForm is disposed, otherwise false.
	 */
	public boolean isDisposed() {
		return (sashForm == null || sashForm.isDisposed());
	}

	/**
	 * Retrieves the ChatMessageArea component.
	 *
	 * @return The ChatMessageArea component used for displaying messages.
	 */
	public ChatConversationArea getChatMessageArea() {
		return chatMessageArea;
	}

	/**
	 * Retrieves the UserInputArea component.
	 *
	 * @return The UserInputArea component used for user input.
	 */
	public UserInputArea getUserInputArea() {
		return userInputArea;
	}

	/**
	 * Enables or disables user interaction components based on the specified flag.
	 *
	 * @param enabled true to enable interaction, false to disable it.
	 */
	public void setInputEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			chatMessageArea.setEnabled(enabled); // Blocks Javascript callbacks.
			userInputArea.setEnabled(enabled);
			buttonBarArea.setInputEnabled(enabled);
		});
	}

	/**
	 * Updates the state of all buttons based on current conditions.
	 */
	public void updateButtonStates() {
		buttonBarArea.updateButtonStates();
	}

	/**
	 * Creates and configures the main container for this view.
	 *
	 * @param parent The parent composite to which this new container will be added. It provides a context
	 *               in which the new composite will be displayed.
	 * @return A newly created and configured Composite instance that serves as the main container for other
	 *         UI components in this view.
	 */
	private Composite createMainContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(1, false, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, -1, Constants.DEFAULT_INTERNAL_SPACING));
		return container;
	}

	/**
	 * Attempts to find an instance of MainView in the active workbench window.
	 *
	 * @return An Optional containing the MainView if available and not disposed,
	 *         otherwise an empty Optional.
	 * @throws IllegalStateException if no active workbench window can be found.
	 */
	public static Optional<MainView> findMainView() {

		// Sometimes getActiveWorkbenchWindow() returns null, so have to do this!!!
		IWorkbenchWindow workbenchWindow = Eclipse.getActiveWorkbenchWindow();
		if (workbenchWindow == null) {
			IWorkbenchWindow[] allWindows = Eclipse.getWorkbenchWindows();
			for (IWorkbenchWindow window : allWindows) {
				workbenchWindow = window;
				if (workbenchWindow != null) {
					break;
				}
			}
		}

		if (workbenchWindow == null) {
			throw new IllegalStateException("Could not retrieve workbench window");
		}

		// Try to get the ChatConversationView.
		IWorkbenchPage activePage = workbenchWindow.getActivePage();
		Optional<MainView> mainView = Optional.ofNullable((MainView) activePage.findView(ID));

		// We don't care if the main SashForm has been disposed of.
		if (mainView.isPresent() && !mainView.get().isDisposed()) {
			return mainView;
		}

		return Optional.empty();
	}

}
