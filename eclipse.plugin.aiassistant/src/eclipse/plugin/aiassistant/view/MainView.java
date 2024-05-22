package eclipse.plugin.aiassistant.view;

import org.eclipse.swt.widgets.Composite;

import java.util.Optional;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.utility.Eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;

/**
 * This class represents a view part that displays a chat conversation. It
 * includes various buttons for user interaction, such as stopping the AI,
 * clearing the chat history, toggling context, and opening settings.
 */
public class MainView extends ViewPart {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "eclipse.plugin.aiassistant.view.MainView";

	private MainPresenter mainPresenter;	

	private SashForm sashForm;
	private Composite mainContainer;
	private ChatConversationArea chatMessageArea;
	private UserInputArea userInputArea;
	private ButtonBarArea buttonBarArea;
		
	/**
	 * Creates the part control for the view. This includes creating the main
	 * container, chat view, user input area, and buttons.
	 * 
	 * @param parent the parent composite
	 */
	@Override
	public void createPartControl(Composite parent) {
		mainPresenter = new MainPresenter();
		sashForm = new SashForm(parent, SWT.VERTICAL);
		mainContainer = createMainContainer(sashForm);
		chatMessageArea =  new ChatConversationArea(mainPresenter, mainContainer);
		userInputArea = new UserInputArea(mainPresenter, mainContainer);
		buttonBarArea = new ButtonBarArea(mainPresenter, mainContainer);
		setInputEnabled(true); // Will turn off stop and set everything else on.
	}
	
	/**
	 * This method is called when the view is being disposed. It shuts down the keepAliveService,
	 * which is a service that keeps the AI assistant alive by sending periodic requests to the server.
	 * It also calls the superclass's dispose() method to perform any necessary cleanup.
	 */
	@Override
	public void dispose() {
		//keepAliveService.shutdown();
		super.dispose();
	}
	
	/**
	 * This method is used by the AbstractPromptHandler to get the MainPresenter instance.
	 * The MainPresenter is responsible for handling the presentation logic of the main view.
	 * 
	 * @return The MainPresenter instance.
	 */
	public MainPresenter getMainPresenter() {
		return mainPresenter;
	}

	/**
	 * Sets the focus on the UserInputArea component of this view.
	 */
	@Override
	public void setFocus() {
		Eclipse.runOnUIThreadAsync(() -> userInputArea.setFocus());
	}

	/**
	 * Checks if the SashForm component of this view is disposed.
	 * 
	 * @return true if the SashForm is disposed, false otherwise
	 */
	public boolean isDisposed() {
		return (sashForm == null || sashForm.isDisposed());
	}

	/**
	 * Retrieves the ChatMessageArea component of this view.
	 * 
	 * @return the ChatMessageArea component of this view
	 */
	public ChatConversationArea getChatMessageArea() {
		return chatMessageArea;
	}

	/**
	 * Retrieves the UserInputArea component of this view.
	 * 
	 * @return the UserInputArea component of this view
	 */
	public UserInputArea getUserInputArea() {
		return userInputArea;
	}
	
	/**
	 * Enables or disables the UserInputArea, ChatMessageArea, and all buttons in
	 * this view. If disabled, the cursor changes to a wait cursor and only the Stop
	 * button is clickable.
	 * 
	 * @param b true to enable the input and buttons, false to disable them
	 */
	public void setInputEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			chatMessageArea.setEnabled(enabled);	// Blocks Javascript callbacks.
			userInputArea.setEnabled(enabled);
			buttonBarArea.setInputEnabled(enabled);
		});
	}

	private Composite createMainContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(1, false, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, -1, Constants.DEFAULT_INTERNAL_SPACING ));
		return container;
	}
	
	/**
	 * Finds the MainView in the application model by its element ID.
	 * 
	 * This method attempts to retrieve the MainView from the active workbench
	 * window and page. If the MainView does not exist or has been disposed of, an
	 * empty Optional is returned.
	 * 
	 * @return An Optional containing the MainView if it exists and has not been
	 *         disposed of, otherwise an empty Optional.
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
