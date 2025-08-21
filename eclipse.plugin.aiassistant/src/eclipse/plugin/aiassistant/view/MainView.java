package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.chat.ChatConversation;
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
	private CTabFolder tabFolder;
	private TabButtonBarArea tabButtonBarArea;
	private List<ChatConversationArea> chatAreas;
	private UserInputArea userInputArea;
	private ButtonBarArea buttonBarArea;

	private Image tabIcon;

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

		// Create tab folder directly in main container
		tabIcon = Eclipse.loadIcon("Robot.png");
		tabFolder = createTabFolder(mainContainer);
		tabButtonBarArea = new TabButtonBarArea(mainPresenter, tabFolder);
		tabFolder.setTopRight(tabButtonBarArea.getButtonContainer()); // Place button container in tab folder header
		chatAreas = new ArrayList<>();

		setupTabListeners();

		userInputArea = new UserInputArea(mainPresenter, mainContainer);
		buttonBarArea = new ButtonBarArea(mainPresenter, mainContainer);
		setInputEnabled(true); // Will turn off stop and set everything else on.
		mainPresenter.loadStateFromPreferenceStore();
	}

	/**
	 * Saves the current state when the view is disposed.
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (tabIcon != null && !tabIcon.isDisposed()) {
			tabIcon.dispose();
		}
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
	 * Retrieves the currently active ChatConversationArea component.
	 *
	 * @return The current ChatConversationArea component used for displaying messages.
	 * @throws IllegalStateException if no chat areas exist
	 */
	public ChatConversationArea getCurrentChatArea() {
		if (chatAreas.isEmpty()) {
			throw new IllegalStateException("No chat areas available");
		}
		int selectedIndex = tabFolder.getSelectionIndex();
		if (selectedIndex < 0 || selectedIndex >= chatAreas.size()) {
			throw new IndexOutOfBoundsException("Tab index " + selectedIndex + " is out of bounds (size: " + chatAreas.size() + ")");
		}
		return chatAreas.get(selectedIndex);
	}

	/**
	 * Retrieves the ChatConversationArea component at the specified tab index.
	 *
	 * @param index The index of the chat area to retrieve
	 * @return The ChatConversationArea at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	public ChatConversationArea getChatAreaAt(int tabIndex) {
		if (tabIndex < 0 || tabIndex >= chatAreas.size()) {
			throw new IndexOutOfBoundsException("Tab index " + tabIndex + " is out of bounds (size: " + chatAreas.size() + ")");
		}
		return chatAreas.get(tabIndex);
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
	 * Creates a new tab using the provided conversation's title.
	 *
	 * @param conversation the conversation to create a tab for
	 */
	public void createNewTab(ChatConversation conversation) {
		String tabName = conversation.getTitle();

		CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);
		tabItem.setText(tabName);

		// Set the conversation icon
		tabItem.setImage(tabIcon);

		// Create a container for the chat area within the tab
		Composite tabContentContainer = new Composite(tabFolder, SWT.NONE);
		tabContentContainer.setLayout(Eclipse.createGridLayout(1, false, 0, 0, 0, 0));

		// Create the chat conversation area
		ChatConversationArea chatArea = new ChatConversationArea(mainPresenter, tabContentContainer);
		chatAreas.add(chatArea);

		tabItem.setControl(tabContentContainer);
		tabFolder.setSelection(tabItem);
	}

	/**
	 * Removes a tab and its associated chat area at the specified index.
	 *
	 * @param tabIndex the index of the tab to remove
	 */
	public void removeTab(int tabIndex) {
		if (tabIndex >= 0 && tabIndex < chatAreas.size()) {
			// Dispose the chat area
			chatAreas.get(tabIndex).getBrowser().dispose();
			chatAreas.remove(tabIndex);

			// Remove the tab item
			tabFolder.getItem(tabIndex).dispose();
		}
	}

	/**
	 * Selects the tab at the specified index.
	 *
	 * @param tabIndex the index of the tab to select
	 */
	public void selectTab(int tabIndex) {
		if (tabIndex >= 0 && tabIndex < tabFolder.getItemCount()) {
			tabFolder.setSelection(tabIndex);
		}
	}

	/**
	 * Updates the title of the tab at the specified index.
	 *
	 * @param tabIndex the index of the tab to update
	 * @param title the new title for the tab
	 */
	public void updateTabTitle(int tabIndex, String title) {
		if (tabIndex >= 0 && tabIndex < tabFolder.getItemCount()) {
			tabFolder.getItem(tabIndex).setText(title);
		}
	}

	/**
	 * Enables or disables user input across the entire view interface.
	 * When input is disabled, the Stop button remains enabled to allow
	 * cancellation of ongoing operations.
	 *
	 * @param enabled true to enable user input, false to disable it
	 */
	public void setInputEnabled(boolean enabled) {
		setInputEnabled(enabled, true);
	}

	/**
	 * Sets the view to a busy/waiting state where all user interactions
	 * are disabled, including the Stop button. This is used for operations
	 * that cannot be cancelled once started.
	 *
	 * @param isBusy true to set busy state (disable all input), false to restore normal state
	 */
	public void setBusyWait(boolean isBusy) {
		setInputEnabled(!isBusy, false);
	}

	/**
	 * Controls the enabled/disabled state of all UI components in the view.
	 * This method coordinates the state of multiple UI areas to ensure consistent
	 * user interaction behavior during different application states.
	 *
	 * @param enabled true to enable user input, false to disable it
	 * @param invertStop when true, the Stop button will have the opposite enabled state
	 *                   of other buttons (enabled when others are disabled, and vice versa).
	 *                   When false, all buttons including Stop follow the same enabled state.
	 */
	private void setInputEnabled(boolean enabled, boolean invertStop) {
		// Show wait cursor when disabled to indicate processing state
		// NOTE: Call first and synchronously, so buttonBarArea can override for "STOP" button.
		Eclipse.runOnUIThreadSync(() -> {
			mainContainer.setCursor(enabled ? null : Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
		});
		Eclipse.runOnUIThreadAsync(() -> {
			getCurrentChatArea().setEnabled(enabled); // Blocks Javascript callbacks.
			userInputArea.setEnabled(enabled);
			buttonBarArea.setInputEnabled(enabled, invertStop);
			tabFolder.setEnabled(enabled); // Block tab switching during operations
			tabButtonBarArea.setInputEnabled(enabled);
		});
	}

	/**
	 * Updates the state of all buttons based on current conditions.
	 */
	public void updateButtonStates() {
		buttonBarArea.updateButtonStates();
		tabButtonBarArea.updateButtonStates();
		userInputArea.updateButtonStates();
	}

	/**
	 * Creates and configures the main container for this view.
	 *
	 * @param parent The parent composite to which this new container will be added.
	 * @return A newly created and configured Composite instance that serves as the main container.
	 */
	private Composite createMainContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(1, false, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, -1, Constants.DEFAULT_INTERNAL_SPACING));
		return container;
	}

	/**
	 * Creates and configures the tab folder for chat conversations.
	 *
	 * @param parent The parent composite for the tab folder.
	 * @return The created CTabFolder instance.
	 */
	private CTabFolder createTabFolder(Composite parent) {
		CTabFolder folder = new CTabFolder(parent, SWT.BORDER);
		folder.setLayoutData(Eclipse.createGridData(true, true));
		folder.setSimple(false);
		folder.setUnselectedCloseVisible(false);
		return folder;
	}

	/**
	 * Sets up listeners for tab selection and closing events.
	 */
	private void setupTabListeners() {
		// Selection listener for tab switching
		tabFolder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			int newIndex = tabFolder.getSelectionIndex();
			mainPresenter.onTabSwitched(newIndex);
		}));

		// Mouse listener for double-click to rename tabs
		tabFolder.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
			@Override
			public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
				CTabItem item = tabFolder.getItem(new org.eclipse.swt.graphics.Point(e.x, e.y));
				if (item != null) {
					int tabIndex = tabFolder.indexOf(item);
					if (tabIndex >= 0) {
						openRenameTabDialog(tabIndex);
					}
				}
			}
		});

		// Tab folder listener for close events
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				int tabIndex = tabFolder.indexOf((CTabItem) event.item);
				boolean shouldClose = mainPresenter.onAttemptCloseTab(tabIndex);

				if (shouldClose) {
					if (tabIndex < 0 || tabIndex >= chatAreas.size()) {
						throw new IndexOutOfBoundsException(
								"Tab index " + tabIndex + " is out of bounds (size: " + chatAreas.size() + ")");
					}

					// Remove from our chat areas list
					chatAreas.remove(tabIndex);

					// Update data model AFTER UI is updated
					mainPresenter.onCloseTab(tabIndex);
				} else {
					// Cancel the close operation
					event.doit = false;
				}
			}
		});
	}

	/**
	 * Shows a dialog to rename the tab at the specified index.
	 *
	 * @param tabIndex the index of the tab to rename
	 */
	private void openRenameTabDialog(int tabIndex) {
		if (tabIndex < 0 || tabIndex >= tabFolder.getItemCount()) {
			return;
		}

		CTabItem tabItem = tabFolder.getItem(tabIndex);
		String currentName = tabItem.getText();

		Eclipse.runOnUIThreadAsync(() -> {
			org.eclipse.jface.dialogs.InputDialog dialog = new org.eclipse.jface.dialogs.InputDialog(
					sashForm.getShell(),
					"Rename Tab",
					"Enter new tab name:",
					currentName,
					null
					);

			if (dialog.open() == org.eclipse.jface.window.Window.OK) {
				String newName = dialog.getValue();
				if (newName != null) {
					// Update through presenter to keep conversation title in sync
					mainPresenter.onTabRenamed(tabIndex, newName.trim());
				}
			}
		});
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