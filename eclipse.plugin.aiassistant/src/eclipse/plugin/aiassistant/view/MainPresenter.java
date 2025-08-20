package eclipse.plugin.aiassistant.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

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

	private final ChatConversation chatConversation;
	private UserMessageHistory userMessageHistory;

	// Used for scrolling through messages using Shift+Scrollwheel combo.
	private UUID currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;

	/**
	 * Initializes the MainPresenter by setting up the chat conversation, API client,
	 * and streaming job. It also sets up listeners for log messages, property changes
	 * (font size), and application shutdown to ensure proper state management.
	 */
	public MainPresenter(MainView mainView) {
		this.mainView = mainView;
		chatConversation = new ChatConversation();
		openAiApiClient = new OpenAiApiClient();
		sendConversationJob = new StreamingChatProcessorJob(this, openAiApiClient, chatConversation);
		userMessageHistory = new UserMessageHistory();
		setupLogListener();
		setupPropertyChangeListener();
		setupShutdownListener();
	}

	/**
	 * Scrolls to the top of the chat message area.
	 */
	public void onScrollToTop() {
		currentlyScrolledToMessageId = SCROLLED_TO_TOP;
		performOnMainViewAsync(mainView -> {
			mainView.getChatMessageArea().scrollToTop();
		});
	}

	/**
	 * Scrolls to the bottom of the chat message area.
	 */
	public void onScrollToBottom() {
		currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;
		performOnMainViewAsync(mainView -> {
			mainView.getChatMessageArea().scrollToBottom();
		});
	}

	/**
	 * Scrolls up in the chat message area. If there is no previous message, scrolls
	 * to the top of the chat message area.
	 */
	public void onScrollUp() {
		if (!currentlyScrolledToMessageId.equals(SCROLLED_TO_TOP)) {
			ChatMessage message;
			if (currentlyScrolledToMessageId.equals(SCROLLED_TO_BOTTOM)) {
				message = chatConversation.getLastMessage();
			} else {
				message = chatConversation.getPreviousMessage(currentlyScrolledToMessageId);
			}
			if (message == null) {
				onScrollToTop();
			} else {
				currentlyScrolledToMessageId = message.getId();
				performOnMainViewAsync(mainView -> {
					mainView.getChatMessageArea().scrollToMessage(currentlyScrolledToMessageId);
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
				message = chatConversation.getFirstMessage();
			} else {
				message = chatConversation.getNextMessage(currentlyScrolledToMessageId);
			}
			if (message == null) {
				onScrollToBottom();
			} else {
				currentlyScrolledToMessageId = message.getId();
				performOnMainViewAsync(mainView -> {
					mainView.getChatMessageArea().scrollToMessage(currentlyScrolledToMessageId);
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
			if (chatConversation.contains(currentlyScrolledToMessageId)) {
				mainView.getChatMessageArea().setSelectionBorder(currentlyScrolledToMessageId);
			}
		});
	}

	/**
	 * Removes all selection borders when the Shift key is released.
	 */
	public void onShiftKeyReleased() {
		performOnMainViewAsync(mainView -> {
			mainView.getChatMessageArea().removeAllSelectionBorders();
		});
	}

	/**
	 * Handles the up arrow click event by retrieving and displaying the previous
	 * user message, if any.
	 */
	public void onUpArrowClick() {
		String previousUserMessage = userMessageHistory.getPreviousMessage();
		if ((previousUserMessage != null)) {
			setUserInputText(previousUserMessage);
		}
	}

	/**
	 * Handles the down arrow click event by retrieving and displaying the next user
	 * message, if any. If there are no more messages, it clears the input area for
	 * a new message.
	 */
	public void onDownArrowClick() {
		// NOTE: If no next message then a new blank will be added beyond the end.
		String nextUserMessage = userMessageHistory.getNextMessage();
		setUserInputText(nextUserMessage != null ? nextUserMessage : "");
	}

	/**
	 * Clears the entire chat conversation and stops any running operations.
	 * Updates the view synchronously to avoid operation ordering issues.
	 */
	public void onClear() {
		if (!chatConversation.isEmpty()) {
			onStop();
			chatConversation.clear();
			performOnMainViewSync(mainView -> {
				mainView.getChatMessageArea().initialize(); // Sync to avoid operation ordering issues
			});
			refreshAfterStatusChange();
		}
	}

	/**
	 * Undoes the last action in the chat conversation by removing the most recent
	 * messages and updating the UI to reflect the changes.
	 */
	public void onUndo() {
		if (!chatConversation.isEmpty()) {
			onStop();
			Iterable<ChatMessage> removedMessages = chatConversation.undo();
			performOnMainViewAsync(mainView -> {
				for (ChatMessage message : removedMessages) {
					mainView.getChatMessageArea().removeMessage(message.getId());
				}
			});
			refreshAfterStatusChange();
		}
	}

	/**
	 * Redoes the last undone action in the chat conversation. It also updates the view
	 * to reflect the changes. Hides the UI during update when redoing from an clear
	 * operation (empty conversation) to avoid flickering when replaying many messages.
	 */
	public void onRedo() {
		if (chatConversation.canRedo()) {
			onStop(); // Stop any running operations
			boolean hideUiDuringUpdate = chatConversation.isEmpty(); // Hide UI when redoing from empty (many messages)
			Iterable<ChatMessage> redoneMessages = chatConversation.redo();
			replayMessages(redoneMessages, hideUiDuringUpdate);
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
			// Clear current conversation (also stop any running operations)
			onClear();

			// Read and deserialize the file
			Path pathObj = Paths.get(path);
			String json = Files.readString(pathObj);

			// Restore the imported conversation and replay to the UI
			chatConversation.resetTo(ChatConversation.fromJson(json));
			replayMessages(chatConversation.getMessages(), true);
		}, "import chat history");
	}

	/**
	 * Exports the current chat conversation and its redo history to a JSON file.
	 */
	public void onExport() {
		if (!chatConversation.isEmpty()) {
			String filePath = Eclipse.showFileDialog(SWT.SAVE, "Export Chat History",
					new String[] { "*.json", "*.*" },
					new String[] { "JSON Files (*.json)", "All Files (*.*)" },
					"chat_history.json");

			performFileOperation(filePath, path -> {
				String json = chatConversation.toJson();
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
		if (!chatConversation.isEmpty()) {
			String filePath = Eclipse.showFileDialog(SWT.SAVE, "Export Chat History as Markdown",
					new String[] { "*.txt", "*.md", "*.*" },
					new String[] { "Text Files (*.txt)", "Markdown Files (*.md)", "All Files (*.*)" },
					"chat_history.txt");

			performFileOperation(filePath, path -> {
				String markdown = chatConversation.toMarkdown();
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
		});
		refreshAfterStatusChange();
	}

	/**
	 * Checks if there the conversation is empty
	 *
	 * @return true if the conversation is empty, false otherwise
	 */
	public boolean isConversationEmpty() {
		return chatConversation.isEmpty();
	}

	/**
	 * Checks if there are any actions available to redo.
	 *
	 * @return true if redo actions are available, false otherwise
	 */
	public boolean canRedo() {
		return chatConversation.canRedo();
	}

	/**
	 * Saves the current chat conversation with its redo history and user
	 * message history to the preference store synchronously on the UI thread.
	 */
	public void saveStateToPreferenceStore() {
		Eclipse.runOnUIThreadSync(new Runnable() {
			@Override
			public void run() {
				try {
					Preferences.saveChatConversation(chatConversation);
				} catch (IOException e) {
					Logger.warning("Failed to save chat conversation: " + e.getMessage());
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
	 * Loads the chat conversation with undo/redo history and user message history
	 * from the preference store. Uses empty message history fallback if loading fails.
	 */
	public void loadStateFromPreferenceStore() {
		Eclipse.runOnUIThreadAsync(new Runnable() {
			@Override
			public void run() {
				try {
					// Restore the loaded conversation and replay to the UI
					chatConversation.resetTo(Preferences.loadChatConversation());
					replayMessages(chatConversation.getMessages(), true);
				} catch (IOException e) {
					Logger.warning("Failed to load chat conversation: " + e.getMessage());
				}
				try {
					userMessageHistory = Preferences.loadUserMessageHistory();
				} catch (IOException e) {
					Logger.warning("Failed to load user message history: " + e.getMessage());
					userMessageHistory = new UserMessageHistory(); // Fallback to an empty message history
				}
			}
		});
	}

	/**
	 * Creates and adds a new empty assistant message to the conversation and disables
	 * user input to indicate an assistant response is in progress.
	 *
	 * @return the newly created ChatMessage
	 */
	public ChatMessage beginMessageFromAssistant() {
		ChatMessage message = new ChatMessage(ChatRole.ASSISTANT);
		performOnMainViewAsync(mainView -> {
			mainView.setInputEnabled(false);
		});
		pushMessage(message);
		return message;
	}

	/**
	 * Updates an existing message from the assistant and maintains scroll position
	 * if user was already at the bottom.
	 *
	 * @param message the message to update
	 */
	public void updateMessageFromAssistant(ChatMessage message) {
		AtomicReference<Boolean> wasAtBottom = new AtomicReference<>(false);
		// NOTE: This needs to be synchronous or otherwise fails to set the flag...
		performOnMainViewSync(mainView -> {
			wasAtBottom.set(mainView.getChatMessageArea().isScrollbarAtBottom());
			mainView.getChatMessageArea().updateMessage(message);
		});
		if (wasAtBottom.get()) {
			currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;
			performOnMainViewSync(mainView -> {
				mainView.getChatMessageArea().scrollToBottom(); // Sync to allow update for next wasAtBottom test
			});
		}
	}

	/**
	 * Completes an assistant message response by re-enabling the user input area.
	 */
	public void endMessageFromAssistant() {
		performOnMainViewAsync(mainView -> {
			mainView.setInputEnabled(true);
		});
	}

	/**
	 * Processes a predefined prompt by incorporating current user input as context,
	 * then sends alternating user and assistant messages. Automatically schedules
	 * an assistant reply if the prompt ends with a blank assistant message.
	 *
	 * @param type the type of the predefined prompt to process
	 */
	public void sendPredefinedPrompt(Prompts type) {

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
		if (scheduleReply && chatConversation.hasUnsentUserMessages()) {
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
		if (scheduleReply && chatConversation.hasUnsentUserMessages()) {
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
		setUserInputText("");
		return currentUserText.get();
	}

	/**
	 * Adds a message to the conversation, displays it in the UI, scrolls to the bottom,
	 * and updates button states.
	 *
	 * @param message the message to add and display
	 */
	private void pushMessage(ChatMessage message) {
		chatConversation.push(message);
		performOnMainViewAsync(mainView -> {
			mainView.getChatMessageArea().newMessage(message);
		});
		refreshAfterStatusChange();
	}

	/**
	 * Replays multiple messages to the UI in sequence. Optionally hides the chat area
	 * during update to prevent flickering when replaying many messages at once.
	 *
	 * @param messages the messages to replay in the UI
	 * @param hideUiDuringUpdate whether to hide the chat area during the update
	 */
	private void replayMessages(Iterable<ChatMessage> messages, boolean hideUiDuringUpdate) {
		if (hideUiDuringUpdate) {
			performOnMainViewAsync(mainView -> {
				mainView.getChatMessageArea().setEnabled(false);
				mainView.getChatMessageArea().setVisible(false);
			});
		}
		performOnMainViewAsync(mainView -> {
			for (ChatMessage message : messages) {
				mainView.getChatMessageArea().newMessage(message);
			}
		});
		refreshAfterStatusChange();
		if (hideUiDuringUpdate) {
			performOnMainViewAsync(mainView -> {
				mainView.getChatMessageArea().setVisible(true);
				mainView.getChatMessageArea().setEnabled(true);
			});
		}
	}

	/**
	 * Sets the text in the user input area asynchronously.
	 *
	 * @param text the text to set in the user input area
	 */
	private void setUserInputText(String text) {
		performOnMainViewAsync(mainView -> {
			mainView.getUserInputArea().setText(text);
		});
	}

	/**
	 * Refreshes the UI state after a status change by scrolling to the bottom
	 * and updating button states to reflect the current conversation state.
	 */
	private void refreshAfterStatusChange() {
		onScrollToBottom();
		performOnMainViewAsync(mainView -> {
			mainView.updateButtonStates();
		});
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

}