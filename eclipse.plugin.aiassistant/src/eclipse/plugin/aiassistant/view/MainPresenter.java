package eclipse.plugin.aiassistant.view;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
 * interactions with the chat conversation. It also handles user interactions
 * such as sending messages, undoing actions, clearing the chat, and stopping
 * the current process.
 */
public class MainPresenter {

	private final static UUID SCROLLED_TO_TOP = new UUID(0, 0); // At top, before the first message.
	private final static UUID SCROLLED_TO_BOTTOM = new UUID(-1, -1); // at bottom, beyond the last message.
	
	private final ChatConversation chatConversation;
	private final OpenAiApiClient openAiApiClient;
	private final StreamingChatProcessorJob sendConversationJob;

	private UserMessageHistory userMessageHistory;

	// Used for scrolling through messages using Shift+Scrollwheel combo.
	private UUID currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;

	/**
	 * Initializes the MainPresenter by setting up a new StreamingChatProcessorJob.
	 * It also sets up a log listener to display
	 * notification messages and a property change listener to handle font size
	 * changes and model selection.
	 */
	public MainPresenter() {
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
		performOnMainView(mainView -> {
			mainView.getChatMessageArea().scrollToTop();
		});
	}

	/**
	 * Scrolls to the bottom of the chat message area.
	 */
	public void onScrollToBottom() {
		currentlyScrolledToMessageId = SCROLLED_TO_BOTTOM;
		performOnMainView(mainView -> {
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
				performOnMainView(mainView -> {
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
				performOnMainView(mainView -> {
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
		performOnMainView(mainView -> {
			if (chatConversation.contains(currentlyScrolledToMessageId)) {
				mainView.getChatMessageArea().setSelectionBorder(currentlyScrolledToMessageId);
			}
		});
	}

	/**
	 * Removes all selection borders when the Shift key is released.
	 */
	public void onShiftKeyReleased() {
		performOnMainView(mainView -> {
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
			performOnMainView(mainView -> {
				mainView.getUserInputArea().setText(previousUserMessage);
			});
		}
	}

	/**
	 * Handles the down arrow click event by retrieving and displaying the next user
	 * message, if any. If there are no more messages, it clears the input area for
	 * a new message.
	 */
	public void onDownArrowClick() {
		String nextUserMessage = userMessageHistory.getNextMessage();
		if (nextUserMessage != null) {
			performOnMainView(mainView -> {
				mainView.getUserInputArea().setText(nextUserMessage);
			});
		} else {
			performOnMainView(mainView -> {
				mainView.getUserInputArea().setText(""); // New blank beyond the end.
			});
		}
	}

	/**
	 * Undoes the last action in the chat conversation. It also updates the view to
	 * reflect the changes.
	 */
	public void onUndo() {
		if (!chatConversation.isEmpty()) {
			onStop(); // Not really needed as we block undo button now when running...
			List<UUID> removedIds = chatConversation.undo();
			if (chatConversation.isEmpty()) {
				saveStateToPreferenceStore(); // So further "Undo" doesn't resurrect the old chat.
			}
			performOnMainView(mainView -> {
				for (UUID id : removedIds) {
					mainView.getChatMessageArea().removeMessage(id);
				}
			});
			onScrollToBottom();
		}
		else {
			loadStateFromPreferenceStore(); // To revert a misclicked "Clear".
		}
	}

	/**
	 * Clears the entire chat conversation. It also updates the view to reflect the
	 * changes.
	 */
	public void onClear() {
		if (!chatConversation.isEmpty()) {
			onStop(); // Not really needed as we block clear button now when running...
			saveStateToPreferenceStore(); // To allow us to revert a misclicked "Clear" via "Undo".
			chatConversation.clear();
			performOnMainView(mainView -> {
				Eclipse.runOnUIThreadAsync(() -> mainView.getChatMessageArea().initialize());
			});
			onScrollToBottom();
		}
	}

	/**
	 * Cancels all running jobs.
	 */
	public void onStop() {
		sendConversationJob.cancel();
		performOnMainView(mainView -> {
			mainView.setInputEnabled(true);
		});
		onScrollToBottom();
	}

	/**
	 * Saves the current state of the chat conversation and user message history to the preference store.
	 * This method ensures that the data is saved synchronously on the UI thread to avoid concurrency issues.
	 * If saving fails due to an IOException, it logs a warning but does not throw an exception.
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
	 * Loads the chat conversation and user message history from the preference store asynchronously.
	 * Messages are processed based on their designated roles and displayed accordingly.
	 * If loading fails due to an IOException, initializes a new empty conversation and message history.
	 * This method also ensures that the view is scrolled to the bottom after processing.
	 */
	public void loadStateFromPreferenceStore() {
	    Eclipse.runOnUIThreadAsync(new Runnable() {
	        @Override
	        public void run() {
	            ChatConversation tempConversation;
	            try {
	                tempConversation = Preferences.loadChatConversation();
	            } catch (IOException e) {
	                Logger.warning("Failed to load chat conversation: " + e.getMessage());
	                tempConversation = new ChatConversation(); // Fallback to an empty conversation
	            }
	            for (ChatMessage message : tempConversation.messages()) {
	                switch (message.getRole()) {
	                    case USER:
	                        sendUserMessage(message.getMessage(), false);
	                        break;
	                    case ASSISTANT:
	                        sendAutoReplyAssistantMessage(message.getMessage());
	                        break;
	                    case NOTIFICATION:
	                        displayNotificationMessage(message.getMessage());
	                        break;
	                }
	            }
	            onScrollToBottom(); // Ensures the view is scrolled to the bottom after loading messages
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
	 * Begins a new message from the assistant. It also updates the view to reflect
	 * the changes.
	 *
	 * @return the newly created ChatMessage
	 */
	public ChatMessage beginMessageFromAssistant() {
		ChatMessage message = new ChatMessage(ChatRole.ASSISTANT);
		chatConversation.push(message);
		performOnMainView(mainView -> {
			mainView.getChatMessageArea().newMessage(message);
			mainView.setInputEnabled(false);
		});
		onScrollToBottom();
		return message;
	}

	/**
	 * Updates an existing message from the assistant. It also updates the view to
	 * reflect the changes.
	 *
	 * @param message the message to update
	 */
	public void updateMessageFromAssistant(ChatMessage message) {
		AtomicReference<Boolean> needsScrollToBottom = new AtomicReference<>(false);
		performOnMainView(mainView -> {
			boolean wasAtBottom = mainView.getChatMessageArea().isScrollbarAtBottom();
			mainView.getChatMessageArea().updateMessage(message);
			if (wasAtBottom && !mainView.getChatMessageArea().isScrollbarAtBottom()) {
				needsScrollToBottom.set(true);
			}
		});
		if (needsScrollToBottom.get()) {
			onScrollToBottom();
		}
	}

	/**
	 * Ends a message from the assistant. It also updates the view to reflect the
	 * changes.
	 */
	public void endMessageFromAssistant() {
		performOnMainView(mainView -> {
			mainView.setInputEnabled(true);
		});
	}

	/**
	 * Sends a predefined prompt to the chat conversation. If the scheduleReply flag
	 * is set, it schedules a reply from the assistant.
	 *
	 * @param type          the type of the predefined prompt
	 * @param scheduleReply whether to schedule a reply from the assistant
	 */
	public void sendPredefinedPrompt(Prompts type) {

		// See if we have any user input to add to to the context.
		String currentUserText = storeAndRetrieveUserMessage();

		// Create the context.
		Context context = new Context(currentUserText);

		// Perform the substitutions and get the messages (and optional auto-replies).
		String[] messages = PromptLoader.createPredefinedPromptMessage(type, context);

		// Alternate the messages, scheduling a reply is the last message is a blank
		// assistant message.
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
	 * Sends a user message to the chat conversation and schedules a reply if
	 * requested.
	 *
	 * @param messageString the message content
	 * @param scheduleReply whether to schedule a reply from the assistant
	 */
	private void sendUserMessage(String messageString, boolean scheduleReply) {

		// Check model is valid to use if we are going to schedule the reply.
		if (scheduleReply && chatConversation.hasUnsentUserMessages()) {
			String serverStatus = openAiApiClient.getCurrentServerStatus();
			if (!serverStatus.equals("OK")) {
				Logger.error(serverStatus);
				return;
			}
		}
			
		// Don't add blank messages to the chat conversation.
		if (!messageString.trim().isEmpty()) {
			ChatMessage message = new ChatMessage(ChatRole.USER, messageString);
			chatConversation.push(message);
			performOnMainView(mainView -> {
				mainView.getChatMessageArea().newMessage(message);
			});
			onScrollToBottom();
		}

		// Schedule the reply if we have something to send and asked to.
		// NOTE: This can be triggered by blank messages to send buffered ones, etc.
		if (scheduleReply && chatConversation.hasUnsentUserMessages()) {
			sendConversationJob.schedule();
		}

	}

	/**
	 * Sends an auto-reply assistant message to the chat conversation.
	 *
	 * @param messageString the message content
	 */
	private void sendAutoReplyAssistantMessage(String messageString) {
		// TODO: Make more robust against blank messages.
		ChatMessage autoReplyMessage = new ChatMessage(ChatRole.ASSISTANT, messageString);
		chatConversation.push(autoReplyMessage);
		performOnMainView(mainView -> {
			mainView.getChatMessageArea().newMessage(autoReplyMessage);
		});
		onScrollToBottom();
	}

	/**
	 * Displays a notification message in the chat conversation.
	 *
	 * @param text the notification message content
	 */
	private void displayNotificationMessage(String text) {
		ChatMessage message = new ChatMessage(ChatRole.NOTIFICATION, text);
		chatConversation.push(message);
		performOnMainView(mainView -> {
			mainView.getChatMessageArea().newMessage(message);
		});
		onScrollToBottom();
	}

	/**
	 * Stores the current user input and retrieves it for further processing.
	 *
	 * @return the current user input
	 */
	private String storeAndRetrieveUserMessage() {
		AtomicReference<String> returnText = new AtomicReference<>("");
		performOnMainView(mainView -> {
			String currentUserText = mainView.getUserInputArea().getText();
			userMessageHistory.storeMessage(currentUserText);
			mainView.getUserInputArea().setText("");
			returnText.set(currentUserText);
		});
		return returnText.get();
	}
	
	/**
	 * Initializes a log listener that filters and displays notification messages unless they are cancellation exceptions.
	 * This method is crucial for providing real-time feedback to the user through the UI.
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
	 * Registers a property change listener to handle changes in font size preferences.
	 * This listener reacts to changes in chat and notification font sizes by clearing the display to apply the new settings immediately.
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
	 * Sets up a listener to ensure the chat conversation is saved when the workbench is about to shut down.
	 * This method guarantees that the current state is preserved across sessions by saving before the application closes.
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
	 * Performs an action on the MainView if it exists.
	 * 
	 * This method retrieves the MainView from the application model and performs a
	 * given action on it. If the MainView does not exist, this method does nothing.
	 * 
	 * @param action The action to be performed on the MainView. It is a Consumer
	 *               that accepts a MainView as its parameter.
	 */
	private void performOnMainView(Consumer<MainView> action) {
		MainView.findMainView().ifPresent(mainView -> {
			action.accept(mainView);
		});
	}

}
