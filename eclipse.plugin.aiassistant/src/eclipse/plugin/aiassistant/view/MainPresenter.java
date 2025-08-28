package eclipse.plugin.aiassistant.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.chat.ChatRole;
import eclipse.plugin.aiassistant.context.Context;
import eclipse.plugin.aiassistant.jobs.StreamingChatProcessorJob;
import eclipse.plugin.aiassistant.network.OpenAiApiClient;
import eclipse.plugin.aiassistant.preferences.PreferenceConstants;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.prompt.PromptLoader;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * The MainPresenter class is responsible for managing the main view and its
 * interactions with the chat conversation. It handles user interactions
 * such as sending messages, navigation, undo/redo actions, clearing the chat,
 * file import/export, and managing the current process state.
 */
public class MainPresenter {

	private final static UUID SCROLLED_TO_TOP = new UUID(0, 0); // At top, before the first message.
	private final static UUID SCROLLED_TO_BOTTOM = new UUID(-1, -1); // at bottom, beyond the last message.

	private final MainView mainView;
	private final OpenAiApiClient openAiApiClient;
	private final StreamingChatProcessorJob sendConversationJob;

	private final List<ChatConversation> chatConversations;
	private int currentTabIndex = 0;
	private final UserMessageHistory userMessageHistory;

	// Used for scrolling through messages using Shift+Scrollwheel combo.
	private UUID currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;

	/**
	 * Initializes the MainPresenter by setting up the chat conversation, API client,
	 * and streaming job. It also sets up listeners for log messages, property changes
	 * (font size), and application shutdown to ensure proper state management.
	 */
	public MainPresenter(MainView mainView) {
		this.mainView = mainView;
		chatConversations = new ArrayList<>();
		chatConversations.add(new ChatConversation()); // Create initial conversation
		openAiApiClient = new OpenAiApiClient();
		sendConversationJob = new StreamingChatProcessorJob(this, openAiApiClient); // Must call setConversation() before running!
		userMessageHistory = new UserMessageHistory();
		setupLogListener();
		setupPropertyChangeListener();
		setupShutdownListener();
	}

	/**
	 * Gets the currently active chat conversation.
	 *
	 * @return the current ChatConversation
	 */
	private ChatConversation getCurrentConversation() {
		return chatConversations.get(currentTabIndex);
	}

	public UserMessageHistory getUserMessageHistory() {
		return userMessageHistory;
	}

	/**
	 * Handles tab switching by updating the current tab index
	 *
	 * @param newTabIndex the index of the newly selected tab
	 */
	public void onTabSwitched(int newTabIndex) {
		if (newTabIndex >= 0 && newTabIndex < chatConversations.size()) {
			currentTabIndex = newTabIndex;
			performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
		}
	}

	/**
	 * Creates a new chat tab with an empty conversation.
	 */
	public void onNewTab() {
		ChatConversation newConversation = new ChatConversation();
		chatConversations.add(newConversation);

		performOnMainViewAsync(mainView -> { mainView.createNewTab(newConversation); });

		// Switch to the new tab
		currentTabIndex = chatConversations.size() - 1;
		userMessageHistory.resetPosition();

		performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
		onScrollToBottom();
	}

	/**
	 * Creates a new chat tab with a copy of the current conversation. Uses a timing
	 * delay hack to ensure the UI tab is fully created before replaying messages,
	 * as no more reliable synchronization mechanism has been found.
	 */
	public void onCloneTab() {

		// We will notify that we are busy until the delayed replayMessages call resets the status
		performOnMainViewAsync(mainView -> { mainView.setBusyWait(true); });

		// Add a deep copy of the current conversation
		ChatConversation clonedConversation = new ChatConversation();
		clonedConversation.copyFrom(getCurrentConversation());
		clonedConversation.setTitle("Copy of " + getCurrentConversation().getTitle());
		chatConversations.add(clonedConversation);

		// Create synchronously to be sure it is there for out replay
		performOnMainViewAsync(mainView -> { mainView.createNewTab(clonedConversation); });

		// Set current tab top last
		currentTabIndex = chatConversations.size() - 1;

		// Reset the user history
		userMessageHistory.resetPosition();

		// Schedule the actual replay for 2 seconds in the future
		// TODO: Find a better way to do this...
		Eclipse.getDisplay().timerExec(Constants.CLONE_TAB_REPLAY_DELAY_MS, () -> {
			// NOTE: This will also update the busy status, the buttons state, and scroll to bottom
			replayMessages(clonedConversation.getMessages(), currentTabIndex, true);
		});
	}

	/**
	 * Handles tab renaming by updating both the conversation title and the UI.
	 *
	 * @param tabIndex the index of the tab being renamed
	 * @param newTitle the new title for the tab
	 */
	public void onTabRenamed(int tabIndex, String newTitle) {
		if (tabIndex >= 0 && tabIndex < chatConversations.size()) {
			chatConversations.get(tabIndex).setTitle(newTitle);
			performOnMainViewAsync(mainView -> { mainView.updateTabTitle(tabIndex, newTitle); });
		}
	}

	/**
	 * Navigates to the previous tab.
	 */
	public void onPreviousTab() {
		if (chatConversations.size() > 1) {
			currentTabIndex = currentTabIndex - 1;
			if (currentTabIndex < 0) {
				currentTabIndex = chatConversations.size() - 1; // Wrap to last tab
			}
			final int newTabIndex = currentTabIndex;
			performOnMainViewAsync(mainView -> {
				mainView.selectTab(newTabIndex);
				mainView.updateButtonStates();
			});
		}
	}

	/**
	 * Navigates to the next tab.
	 */
	public void onNextTab() {
		if (chatConversations.size() > 1) {
			currentTabIndex = currentTabIndex + 1;
			if (currentTabIndex >= chatConversations.size()) {
				currentTabIndex = 0; // Wrap to first tab
			}
			final int newTabIndex = currentTabIndex;
			performOnMainViewAsync(mainView -> {
				mainView.selectTab(newTabIndex);
				mainView.updateButtonStates();
			});
		}
	}

	/**
	 * Checks if a tab at the specified index can be closed and shows confirmation dialog
	 * if the conversation is not empty. Does not update the data model - that is handled
	 * later by onCloseTab() after the UI has been updated. If the last tab is closed,
	 * a new blank tab will be automatically created inside of `onCloseTab()`.
	 *
	 * @param tabIndex the index of the tab to close
	 * @return true if the tab should be closed, false if cancelled by user
	 */
	public boolean onAttemptCloseTab(int tabIndex) {
		if (tabIndex < 0 || tabIndex >= chatConversations.size()) {
			return false;
		}

		// Show confirmation dialog if conversation is not empty
		if (!chatConversations.get(tabIndex).isEmpty()) {
			boolean confirmed = Eclipse.showConfirmDialog(
					"Close Tab",
					"This conversation is not empty. Are you sure you want to close it?"
					);
			if (!confirmed) {
				return false;
			}
		}

		// Allow closure - data model will be updated in onCloseTab()
		return true;
	}

	/**
	 * Called by the UI after a tab has been successfully removed.
	 * Updates the data model to match the UI state. If all tabs are closed,
	 * automatically creates a new blank tab to ensure at least one tab remains.
	 */
	public void onCloseTab(int closedTabIndex) {
		// Remove the conversation from the data model
		chatConversations.remove(closedTabIndex);

		// Adjust current tab index if necessary
		if (currentTabIndex >= closedTabIndex && currentTabIndex > 0) {
			currentTabIndex--;
		}
		if (currentTabIndex >= chatConversations.size()) {
			currentTabIndex = chatConversations.size() - 1;
		}

		// If we closed all tabs, create a new blank one, otherwise just update button states
		if (chatConversations.isEmpty()) {
			onNewTab();
		} else {
			performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
		}
	}

	/**
	 * Checks if all tabs can be closed by showing a confirmation dialog when there
	 * are multiple tabs or when the current conversation has content. Does not update
	 * the data model - that is handled later by onCloseAllTabs().
	 *
	 * @return true if all tabs should be closed, false if cancelled by user
	 */
	public boolean onAttemptCloseAllTabs() {

		// Show confirmation dialog if there are multiple tabs or current conversation has content
		if (getTabCount() > 1 || !isConversationEmpty()) {
			boolean confirmed = Eclipse.showConfirmDialog(
					"Close All Tabs",
					"Are you sure you want to close all tabs? This will clear all conversations."
					);
			if (!confirmed) {
				return false;
			}
		}

		// Allow closure - data model will be updated in onCloseAllTabs()
		return true;
	}

	/**
	 * Closes all tabs by removing both the UI tabs and their corresponding conversations,
	 * then creates a new blank tab. Uses async execution to perform the removal and
	 * ensures onNewTab is called after all tabs are removed to maintain at least one tab.
	 */
	public void onCloseAllTabs() {
		performOnMainViewAsync(mainView -> {
			// Remove all tabs by repeatedly removing the first one
			while (!chatConversations.isEmpty()) {
				mainView.removeTab(0);
				chatConversations.remove(0);
			}
			// Create a new blank tab after all tabs are removed
			onNewTab();
		});
	}

	/**
	 * Gets the number of chat tabs.
	 *
	 * @return the number of tabs
	 */
	public int getTabCount() {
		return chatConversations.size();
	}

	/**
	 * Gets the current tab index.
	 *
	 * @return the current tab index
	 */
	public int getCurrentTabIndex() {
		return currentTabIndex;
	}

	/**
	 * Handles the clear messages button click. Shows a confirmation dialog
	 * and clears the message history if confirmed.
	 */
	public void onAttemptClearMessages() {
		boolean confirmed = Eclipse.showConfirmDialog(
				"Clear Message History",
				"Are you sure you want to clear all stored user messages?"
				);

		if (confirmed) {
			userMessageHistory.clear();
			performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
		}
	}

	/**
	 * Scrolls to the top of the chat message area.
	 */
	public void onScrollToTop() {
		currentlyScrolledToMessageId = SCROLLED_TO_TOP;
		performOnMainViewAsync(mainView -> { mainView.getCurrentChatArea().scrollToTop(); });
	}

	/**
	 * Scrolls to the bottom of the chat message area.
	 */
	public void onScrollToBottom() {
		currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;
		performOnMainViewAsync(mainView -> { mainView.getCurrentChatArea().scrollToBottom(); });
	}

	/**
	 * Scrolls up in the chat message area. If there is no previous message, scrolls
	 * to the top of the chat message area.
	 */
	public void onScrollUp() {
		if (!currentlyScrolledToMessageId.equals(SCROLLED_TO_TOP)) {
			ChatMessage message;
			if (currentlyScrolledToMessageId.equals(SCROLLED_TO_BOTTOM)) {
				message = getCurrentConversation().getLastMessage();
			} else {
				message = getCurrentConversation().getPreviousMessage(currentlyScrolledToMessageId);
			}
			if (message == null) {
				onScrollToTop();
			} else {
				currentlyScrolledToMessageId = message.getId();
				performOnMainViewAsync(mainView -> {
					mainView.getCurrentChatArea().scrollToMessage(currentlyScrolledToMessageId);
				});
			}
		}
	}

	/**
	 * Scrolls down in the chat message area. If there is no next message, scrolls
	 * to the bottom of the chat message area.
	 */
	public void onScrollDown() {
		if (!currentlyScrolledToMessageId.equals(SCROLLED_TO_BOTTOM)) {
			ChatMessage message;
			if (currentlyScrolledToMessageId.equals(SCROLLED_TO_TOP)) {
				message = getCurrentConversation().getFirstMessage();
			} else {
				message = getCurrentConversation().getNextMessage(currentlyScrolledToMessageId);
			}
			if (message == null) {
				onScrollToBottom();
			} else {
				currentlyScrolledToMessageId = message.getId();
				performOnMainViewAsync(mainView -> {
					mainView.getCurrentChatArea().scrollToMessage(currentlyScrolledToMessageId);
				});
			}
		}
	}

	/**
	 * Sets the selection border on the currently scrolled-to message when the Shift
	 * key is pressed.
	 */
	public void onShiftKeyPressed() {
		performOnMainViewAsync(mainView -> {
			if (getCurrentConversation().contains(currentlyScrolledToMessageId)) {
				mainView.getCurrentChatArea().setSelectionBorder(currentlyScrolledToMessageId);
			}
		});
	}

	/**
	 * Removes all selection borders when the Shift key is released.
	 */
	public void onShiftKeyReleased() {
		performOnMainViewAsync(mainView -> {
			mainView.getCurrentChatArea().removeAllSelectionBorders();
		});
	}

	/**
	 * Handles the up arrow click event by retrieving and displaying the older
	 * user message, if any.
	 */
	public void onUpArrowClick() {
		String olderUserMessage = userMessageHistory.getOlderMessage();
		if ((olderUserMessage != null)) {
			setUserInputText(olderUserMessage);
		}
		performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
	}

	/**
	 * Handles the down arrow click event by retrieving and displaying the newer user
	 * message, if any. If there are no more messages, it clears the input area for
	 * a new message.
	 */
	public void onDownArrowClick() {
		// NOTE: If no newer message then a new blank will be added beyond the end.
		String newerUserMessage = userMessageHistory.getNewerMessage();
		setUserInputText(newerUserMessage != null ? newerUserMessage : "");
		performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
	}

	/**
	 * Clears the entire chat conversation and stops any running operations.
	 * Updates the view synchronously to avoid operation ordering issues.
	 */
	public void onClear() {
		if (!getCurrentConversation().isEmpty()) {
			onStop();
			getCurrentConversation().clear();
			// NOTE: Synchronous to avoid operation ordering issues
			performOnMainViewAsync(mainView -> { mainView.getCurrentChatArea().initialize(); });
			performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
			onScrollToBottom();
		}
	}

	/**
	 * Undoes the last action in the chat conversation by removing the most recent
	 * messages and updating the UI to reflect the changes.
	 */
	public void onUndo() {
		if (!getCurrentConversation().isEmpty()) {
			onStop();
			Iterable<ChatMessage> removedMessages = getCurrentConversation().undo();
			performOnMainViewAsync(mainView -> {
				for (ChatMessage message : removedMessages) {
					mainView.getCurrentChatArea().removeMessage(message.getId());
				}
				mainView.updateButtonStates();
			});
			onScrollToBottom();
		}
	}

	/**
	 * Redoes the last undone action in the chat conversation. It also updates the view
	 * to reflect the changes. Hides the UI during update when redoing from an clear
	 * operation (empty conversation) to avoid flickering when replaying many messages.
	 */
	public void onRedo() {
		if (getCurrentConversation().canRedo()) {
			onStop();
			// NOTE: Only hide the UI when doing a potentially large redo from a cleared state
			boolean hideUiDuringUpdate = getCurrentConversation().isEmpty();
			// NOTE: This will also update the buttons state and scroll to bottom
			Iterable<ChatMessage> redoneMessages = getCurrentConversation().redo();
			replayMessages(redoneMessages, currentTabIndex, hideUiDuringUpdate);
		}
	}

	/**
	 * Imports a chat conversation from a JSON file. This clears the current conversation
	 * and replaces it with the imported conversation and its redo history.
	 */
	public void onImport() {
		String filePath = Eclipse.showFileDialog(SWT.OPEN, "Import Chat History",
				new String[] { "*.json", "*.*" },
				new String[] { "JSON Files (*.json)", "All Files (*.*)" },
				null);

		performFileOperation(filePath, path -> {
			// Clear current conversation (also stops any running operations)
			onClear();

			// Read and deserialize the file
			Path pathObj = Paths.get(path);
			String json = Files.readString(pathObj);

			// NOTE:Update first so replayMessages can update the buttons state...
			userMessageHistory.resetPosition();

			// NOTE: This will also update the buttons state and scroll to bottom
			getCurrentConversation().copyFrom(ChatConversation.fromJson(json));
			replayMessages(getCurrentConversation().getMessages(), currentTabIndex, true);
		}, "import chat history");
	}

	/**
	 * Exports the current chat conversation and its redo history to a JSON file.
	 */
	public void onExport() {
		if (!getCurrentConversation().isEmpty()) {
			String title = getCurrentConversation().getTitle();
			String sanitizedTitle = sanitizeFilename(title);
			String defaultFilename = sanitizedTitle.isEmpty() ? "chat_history" : sanitizedTitle;

			String filePath = Eclipse.showFileDialog(SWT.SAVE, "Export Chat History",
					new String[] { "*.json", "*.*" },
					new String[] { "JSON Files (*.json)", "All Files (*.*)" },
					defaultFilename + ".json");

			performFileOperation(filePath, path -> {
				String json = getCurrentConversation().toJson();
				Path pathObj = Paths.get(path);
				Files.writeString(pathObj, json);
			}, "export chat history");
		}
	}

	/**
	 * Exports the current chat conversation to a Markdown file. Only exports
	 * the USER and ASSISTANT messages, excluding notification messages.
	 */
	public void onExportMarkdown() {
		if (!getCurrentConversation().isEmpty()) {
			String title = getCurrentConversation().getTitle();
			String sanitizedTitle = sanitizeFilename(title);
			String defaultFilename = sanitizedTitle.isEmpty() ? "chat_history" : sanitizedTitle;

			String filePath = Eclipse.showFileDialog(SWT.SAVE, "Export Chat History as Markdown",
					new String[] { "*.txt", "*.md", "*.*" },
					new String[] { "Text Files (*.txt)", "Markdown Files (*.md)", "All Files (*.*)" },
					defaultFilename + ".txt");

			performFileOperation(filePath, path -> {
				String markdown = getCurrentConversation().toMarkdown();
				Path pathObj = Paths.get(path);
				Files.writeString(pathObj, markdown);
			}, "export chat history");
		}
	}

	/**
	 * Cancels the streaming chat processor job and re-enables user input.
	 */
	public void onStop() {
		sendConversationJob.cancel();
		performOnMainViewAsync(mainView -> {
			mainView.setInputEnabled(true);
			mainView.updateButtonStates();
		});
		onScrollToBottom();
	}

	/**
	 * Checks if there the conversation is empty
	 *
	 * @return true if the conversation is empty, false otherwise
	 */
	public boolean isConversationEmpty() {
		return getCurrentConversation().isEmpty();
	}

	/**
	 * Checks if there are any actions available to redo.
	 *
	 * @return true if redo actions are available, false otherwise
	 */
	public boolean canRedo() {
		return getCurrentConversation().canRedo();
	}

	/**
	 * Saves all chat conversations with their redo history and user
	 * message history to the preference store synchronously on the UI thread.
	 */
	public void saveStateToPreferenceStore() {
		Eclipse.runOnUIThreadSync(new Runnable() {
			@Override
			public void run() {
				try {
					Preferences.saveChatConversations(chatConversations);
				} catch (IOException e) {
					Logger.warning("Failed to save chat conversations: " + e.getMessage());
				}
				try {
					Preferences.saveUserMessageHistory(userMessageHistory);
				} catch (IOException e) {
					Logger.warning("Failed to save user message history: " + e.getMessage());
				}
			}
		});
	}

	/**
	 * Loads all chat conversations with undo/redo history and user message history
	 * from the preference store. Creates the appropriate number of tabs and replays
	 * messages to each tab. Uses a timing delay hack to ensure all UI tabs are
	 * fully created before replaying messages, as no more reliable synchronization
	 * mechanism has been found.
	 */
	public void loadStateFromPreferenceStore() {

		// We will notify that we are busy until the delayed replayMessages call resets the status
		performOnMainViewAsync(mainView -> { mainView.setBusyWait(true); });

		// Load user message history from preferences, fall back to empty history on failure
		// NOTE: Do this first so replayMessages can update the buttons state...
		try {
			userMessageHistory.copyFrom(Preferences.loadUserMessageHistory());
		} catch (IOException | ClassNotFoundException e) {
			Logger.warning("Failed to load user message history: " + e.getMessage());
			userMessageHistory.copyFrom(new UserMessageHistory());
		}

		// Load all conversations from preferences, fall back to empty list on failure
		List<ChatConversation> loadedConversations;
		try {
			loadedConversations = Preferences.loadChatConversations();
		} catch (IOException | ClassNotFoundException e) {
			Logger.warning("Failed to load chat conversations: " + e.getMessage());
			loadedConversations = new ArrayList<>();
		}

		// Replace current conversations
		chatConversations.clear();
		chatConversations.addAll(loadedConversations);

		// Create the tabs we are going to need
		performOnMainViewAsync(mainView -> {
			for (ChatConversation conversation : chatConversations) {
				mainView.createNewTab(conversation);
			}
		});

		// Set current tab top last
		currentTabIndex = chatConversations.size() - 1;

		// Schedule the actual replay for 5 seconds in the future
		// TODO: Find a better way to do this...
		for (int i = 0; i < chatConversations.size(); i++) {
			final int index = i;
			Eclipse.getDisplay().timerExec(Constants.STATE_RESTORE_REPLAY_DELAY_MS, () -> {
				// NOTE: This will also update the busy status, the buttons state, and scroll to bottom
				replayMessages(chatConversations.get(index).getMessages(), index, true);
			});
		}
	}

	/**
	 * Creates and adds a new empty assistant message to the conversation and disables
	 * user input to indicate an assistant response is in progress.
	 *
	 * @return the newly created ChatMessage
	 */
	public ChatMessage beginMessageFromAssistant() {
		ChatMessage message = new ChatMessage(ChatRole.ASSISTANT);
		performOnMainViewAsync(mainView -> { mainView.setInputEnabled(false); });
		pushMessage(message);
		return message;
	}

	/**
	 * Updates an existing message from the assistant and maintains scroll position.
	 * If the user was viewing the bottom of the chat, keeps them at the bottom
	 * to show new content. If they were scrolled up, preserves their position.
	 *
	 * @param message the message to update
	 */
	public void updateMessageFromAssistant(ChatMessage message) {
		AtomicReference<Boolean> scrollbarAtBottom = new AtomicReference<>(false);
		performOnMainViewSync(mainView -> {
			scrollbarAtBottom.set(mainView.getCurrentChatArea().isScrollbarAtBottom());
		});
		performOnMainViewAsync(mainView -> {
			mainView.getCurrentChatArea().updateMessage(message);
		});
		if (scrollbarAtBottom.get()) {
			onScrollToBottom();
		}
	}

	/**
	 * Completes an assistant message response by re-enabling the user input area.
	 */
	public void endMessageFromAssistant() {
		performOnMainViewAsync(mainView -> {
			mainView.setInputEnabled(true);
			mainView.updateButtonStates();
		});
	}

	/**
	 * Processes a predefined prompt by incorporating current user input as context,
	 * then sends alternating user and assistant messages. Automatically schedules
	 * an assistant reply if the prompt ends with a blank assistant message.
	 *
	 * This method prevents concurrent execution by checking if the conversation job is
	 * already scheduled or running.
	 *
	 * @param type the type of the predefined prompt to process
	 */
	public void sendPredefinedPrompt(Prompts type) {

		// Check if we're already processing (job scheduled/waiting/running)
		if (sendConversationJob.getState() != Job.NONE) {
			return;
		}

		// See if we have any user input to add to to the context.
		String currentUserText = storeAndRetrieveUserMessage();

		// Create the context.
		Context context = new Context(currentUserText);

		// Perform the substitutions and get the messages (and optional auto-replies).
		String[] messages = PromptLoader.createPredefinedPromptMessage(type, context);

		// Alternate between user and assistant messages, scheduling a reply if
		// the last message is a blank assistant message.
		boolean isUserMessage = true;
		for (int i = 0; i < messages.length; i++) {
			if (isUserMessage) {
				sendUserMessage(messages[i], i == (messages.length - 2) && messages[i + 1].isEmpty());
			} else if (i < (messages.length - 1) || !messages[i].isEmpty()) {
				sendAutoReplyAssistantMessage(messages[i]);
			}
			isUserMessage = !isUserMessage;
		}
	}

	/**
	 * Adds a user message to the conversation (if non-blank) and optionally schedules
	 * an assistant reply. Validates the model status before scheduling and can trigger
	 * replies for previously buffered messages even when the current message is blank.
	 *
	 * @param messageString the message content
	 * @param scheduleReply whether to schedule a reply from the assistant
	 */
	private void sendUserMessage(String messageString, boolean scheduleReply) {

		// Check model is valid to use if we are going to schedule the reply.
		if (scheduleReply && getCurrentConversation().hasUnsentUserMessages()) {
			String modelStatus = openAiApiClient.checkCurrentModelStatus();
			if (!modelStatus.equals("OK")) {
				Logger.error(modelStatus);
				return;
			}
		}

		// Don't add blank messages to the chat conversation.
		if (!messageString.trim().isEmpty()) {
			ChatMessage message = new ChatMessage(ChatRole.USER, messageString);
			pushMessage(message);
		}

		// Schedule the reply if we have something to send and asked to.
		// NOTE: This can be triggered by blank messages to send buffered ones, etc.
		if (scheduleReply && getCurrentConversation().hasUnsentUserMessages()) {
			sendConversationJob.setConversation(getCurrentConversation());
			sendConversationJob.schedule();
		}

	}

	/**
	 * Adds a pre-written assistant message to the conversation without triggering
	 * any API calls or user input validation.
	 *
	 * @param messageString the message content
	 */
	private void sendAutoReplyAssistantMessage(String messageString) {
		// TODO: Make more robust against blank messages.
		ChatMessage autoReplyMessage = new ChatMessage(ChatRole.ASSISTANT, messageString);
		pushMessage(autoReplyMessage);
	}

	/**
	 * Displays a notification message in the chat conversation.
	 *
	 * @param text the notification message content
	 */
	private void displayNotificationMessage(String text) {
		ChatMessage message = new ChatMessage(ChatRole.NOTIFICATION, text);
		pushMessage(message);
	}

	/**
	 * Captures the current user input text, clears the input field, adds the text
	 * to message history, and returns it for further processing.
	 *
	 * @return the captured user input text
	 */
	private String storeAndRetrieveUserMessage() {
		AtomicReference<String> currentUserText = new AtomicReference<>("");
		// NOTE: This needs to be synchronous or otherwise fails to return the text...
		performOnMainViewSync(mainView -> {
			currentUserText.set(mainView.getUserInputArea().getText());
		});
		userMessageHistory.storeMessage(currentUserText.get());
		performOnMainViewAsync(mainView -> { mainView.updateButtonStates(); });
		setUserInputText("");
		return currentUserText.get();
	}

	/**
	 * Adds a message to the conversation and displays it in the UI. Automatically
	 * scrolls to bottom and updates button states after the change.
	 *
	 * @param message the message to add and display
	 */
	private void pushMessage(ChatMessage message) {
		getCurrentConversation().push(message);
		performOnMainViewAsync(mainView -> {
			mainView.getCurrentChatArea().newMessage(message);
			mainView.updateButtonStates();
		});
		onScrollToBottom();
	}

	/**
	 * Replays multiple messages to a specific tab's chat area. Temporarily disables
	 * async execution and optionally hides the UI during replay to prevent flickering.
	 *
	 * @param messages the messages to replay in sequence
	 * @param tabIndex the target tab for message replay
	 * @param hideUiDuringUpdate whether to hide the chat area during replay
	 */
	private void replayMessages(Iterable<ChatMessage> messages, int tabIndex, boolean hideUiDuringUpdate) {
		performOnMainViewAsync(mainView -> {
			mainView.setBusyWait(true);
			mainView.getChatAreaAt(tabIndex).setAsyncExecution(false);
			mainView.getChatAreaAt(tabIndex).setEnabled(false);
			if (hideUiDuringUpdate) {
				mainView.getChatAreaAt(tabIndex).setVisible(false);
			}
			for (ChatMessage message : messages) {
				mainView.getChatAreaAt(tabIndex).newMessage(message);
			}
			mainView.getChatAreaAt(tabIndex).scrollToBottom();
			if (hideUiDuringUpdate) {
				mainView.getChatAreaAt(tabIndex).setVisible(true);
			}
			mainView.getChatAreaAt(tabIndex).setEnabled(true);
			mainView.getChatAreaAt(tabIndex).setAsyncExecution(true);
			mainView.updateButtonStates();
			mainView.setBusyWait(false);
		});
		onScrollToBottom();
	}

	/**
	 * Sets the text in the user input area asynchronously.
	 *
	 * @param text the text to set in the user input area
	 */
	private void setUserInputText(String text) {
		performOnMainViewAsync(mainView -> { mainView.getUserInputArea().setText(text); });
	}

	/**
	 * Initializes a log listener that filters and displays notification messages
	 * unless they are cancellation exceptions. This method is crucial for providing
	 * real-time feedback to the user through the UI.
	 */
	private void setupLogListener() {
		Logger.getDefault().addLogListener(new ILogListener() {
			@Override
			public void logging(IStatus status, String plugin) {
				// Ignore cancellation exceptions to avoid unnecessary user notifications
				if (!status.getMessage().equals("CancellationException")) {
					displayNotificationMessage(status.getMessage());
				}
			}
		});
	}

	/**
	 * Registers a property change listener to handle changes in font size
	 * preferences. This listener reacts to changes in chat and notification font
	 * sizes by clearing the display to apply the new settings immediately.
	 */
	private void setupPropertyChangeListener() {
		Preferences.getDefault().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				// React to changes in font size settings
				if (event.getProperty().equals(PreferenceConstants.CHAT_FONT_SIZE)
						|| event.getProperty().equals(PreferenceConstants.NOTIFICATION_FONT_SIZE)) {
					onClear(); // Clear the display to apply font size changes instantly
				}
			}
		});
	}

	/**
	 * Sets up a listener to ensure the chat conversation is saved when the
	 * workbench is about to shut down. This method guarantees that the current
	 * state is preserved across sessions by saving before the application closes.
	 */
	private void setupShutdownListener() {
		Eclipse.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				saveStateToPreferenceStore(); // Ensure state is saved before shutdown
				return true;  // Allow the shutdown process to continue
			}

			@Override
			public void postShutdown(IWorkbench workbench) {
				// Perform any necessary cleanup after shutdown
			}
		});
	}

	/**
	 * Performs an action on the MainView asynchronously if it exists. The action
	 * is executed on the UI thread without blocking the caller.
	 *
	 * @param action the action to be performed on the MainView
	 */
	private void performOnMainViewAsync(Consumer<MainView> action) {
		if (!mainView.isDisposed()) {
			Eclipse.runOnUIThreadAsync(() -> action.accept(mainView));
		}
	}

	/**
	 * Performs an action on the MainView synchronously if it exists. The action
	 * is executed on the UI thread and blocks the caller until completion.
	 *
	 * @param action the action to be performed on the MainView
	 */
	private void performOnMainViewSync(Consumer<MainView> action) {
		if (!mainView.isDisposed()) {
			Eclipse.runOnUIThreadSync(() -> action.accept(mainView));
		}
	}

	/**
	 * Functional interface for file operations that can throw IOException.
	 */
	@FunctionalInterface
	private interface FileOperation {
		void execute(String filePath) throws IOException;
	}

	/**
	 * Performs a file operation with standardized error handling.
	 *
	 * @param filePath the file path to operate on
	 * @param operation the file operation to perform
	 * @param operationName the operation name for error messages
	 */
	private void performFileOperation(String filePath, FileOperation operation, String operationName) {
		if (filePath != null) {
			try {
				operation.execute(filePath);
			} catch (IOException e) {
				Logger.error("Failed to " + operationName + ": " + e.getMessage());
			} catch (Exception e) {
				Logger.error("Failed to " + operationName + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Sanitizes a string to make it safe for use as a filename by removing
	 * or replacing invalid characters.
	 */
	private static String sanitizeFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// Remove or replace invalid filename characters
		return filename.replaceAll("[/\\\\:*?\"<>|]", "_")
				.replaceAll("\\s+", "_")
				.trim();
	}

}