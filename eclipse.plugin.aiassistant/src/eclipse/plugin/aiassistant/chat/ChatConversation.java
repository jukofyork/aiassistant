package eclipse.plugin.aiassistant.chat;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a chat conversation with undo/redo capabilities.
 */
public class ChatConversation implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final Stack<ChatMessage> messages = new Stack<>();
	private final Stack<List<ChatMessage>> redoStack = new Stack<>();

	/**
	 * Adds a message to the conversation and clears any pending redo operations.
	 * Adding a new message invalidates the redo history since the conversation
	 * has diverged from the previously undone path.
	 *
	 * @param message the message to add
	 */
	public synchronized void push(ChatMessage message) {
		messages.push(message);
		redoStack.clear();
	}

	/**
	 * Clears all messages from the conversation and preserves them for redo.
	 * The cleared messages can be restored using the redo operation.
	 */
	public synchronized void clear() {
		if (!messages.isEmpty()) {
			redoStack.push(new ArrayList<>(messages));
			messages.clear();
		}
	}

	/**
	 * Replaces this conversation's internal state with another conversation's state.
	 * Clears current messages and redo history, then copies both from 'other'.
	 */
	public synchronized void copyFrom(ChatConversation other) {
		messages.clear();
		for (ChatMessage m : other.messages) {
			messages.add(m);
		}

		redoStack.clear();
		for (List<ChatMessage> group : other.redoStack) {
			redoStack.add(new ArrayList<>(group));
		}
	}

	/**
	 * Undoes the last conversation turn by removing messages from the conversation
	 * until reaching a natural break point (user message boundary).
	 *
	 * @return the messages that were removed from the conversation, in reverse
	 *         chronological order (most recent first)
	 */
	public synchronized Iterable<ChatMessage> undo() {
		if (messages.isEmpty()) {
			return Collections.emptyList();
		}

		List<ChatMessage> removedMessages = new ArrayList<>();

		while (!messages.isEmpty()) {
			ChatMessage message = messages.pop();
			removedMessages.add(message);
			if (message.getRole() == ChatRole.USER) {
				break;
			}
			if (!messages.isEmpty() && messages.peek().getRole() == ChatRole.USER) {
				break;
			}
		}

		if (!removedMessages.isEmpty()) {
			List<ChatMessage> reversedMessages = new ArrayList<>(removedMessages);
			Collections.reverse(reversedMessages);
			redoStack.push(reversedMessages);
		}

		return Collections.unmodifiableList(removedMessages);
	}

	/**
	 * Restores the most recently undone group of messages.
	 *
	 * @return an iterable of the messages that were redone
	 */
	public synchronized Iterable<ChatMessage> redo() {
		if (redoStack.isEmpty()) {
			return Collections.emptyList();
		}
		List<ChatMessage> redoneMessages = redoStack.pop();
		messages.addAll(redoneMessages);
		return Collections.unmodifiableList(redoneMessages);
	}

	/**
	 * Returns the number of messages in the conversation.
	 *
	 * @return the total number of messages
	 */
	public synchronized int size() {
		return messages.size();
	}

	/**
	 * Checks if the conversation contains any messages.
	 *
	 * @return true if the conversation is empty, false otherwise
	 */
	public synchronized boolean isEmpty() {
		return messages.isEmpty();
	}

	/**
	 * Checks if there are operations that can be redone.
	 *
	 * @return true if redo operation is available, false otherwise
	 */
	public synchronized boolean canRedo() {
		return !redoStack.isEmpty();
	}

	/**
	 * Checks if a message with the given UUID exists in the conversation.
	 *
	 * @param id the UUID of the message to check
	 * @return true if the message exists, false otherwise
	 */
	public synchronized boolean contains(UUID id) {
		for (ChatMessage message : messages) {
			if (id.equals(message.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if there are user messages that haven't been responded to by the assistant.
	 * Scans backwards from the most recent message to check if a USER message appears before
	 * an ASSISTANT message, indicating unsent user messages awaiting a response.
	 *
	 * @return true if there are unsent user messages, false otherwise
	 */
	public synchronized boolean hasUnsentUserMessages() {
		for (int i = messages.size() - 1; i >= 0; i--) {
			ChatMessage message = messages.get(i);
			if (message.getRole() == ChatRole.USER) {
				return true;
			}
			if (message.getRole() == ChatRole.ASSISTANT) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns the first non-notification message in the conversation.
	 * NOTIFICATION messages are skipped as they are internal application messages.
	 *
	 * @return the first message in the conversation, or null if there are no
	 *         non-notification messages
	 */
	public synchronized ChatMessage getFirstMessage() {
		for (ChatMessage message : messages) {
			if (message.getRole() != ChatRole.NOTIFICATION) {
				return message;
			}
		}
		return null;
	}

	/**
	 * Returns the last non-notification message in the conversation.
	 * NOTIFICATION messages are skipped as they are internal application messages.
	 *
	 * @return the last message in the conversation, or null if there are no
	 *         non-notification messages
	 */
	public synchronized ChatMessage getLastMessage() {
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i).getRole() != ChatRole.NOTIFICATION) {
				return messages.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns the non-notification message that appears before the specified message.
	 * NOTIFICATION messages are skipped during navigation.
	 *
	 * @param id the UUID of the reference message
	 * @return the previous non-notification message, or null if the message is not found
	 *         or there is no previous non-notification message
	 */
	public synchronized ChatMessage getPreviousMessage(UUID id) {
		int index = -1;
		for (int i = 0; i < messages.size(); i++) {
			if (id.equals(messages.get(i).getId())) {
				index = i;
				break;
			}
		}
		for (int i = index - 1; i >= 0; i--) {
			if (messages.get(i).getRole() != ChatRole.NOTIFICATION) {
				return messages.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns the non-notification message that appears after the specified message.
	 * NOTIFICATION messages are skipped during navigation.
	 *
	 * @param id the UUID of the reference message
	 * @return the next non-notification message, or null if the message is not found
	 *         or there is no next non-notification message
	 */
	public synchronized ChatMessage getNextMessage(UUID id) {
		int index = messages.size();
		for (int i = 0; i < messages.size(); i++) {
			if (id.equals(messages.get(i).getId())) {
				index = i;
				break;
			}
		}
		for (int i = index + 1; i < messages.size(); i++) {
			if (messages.get(i).getRole() != ChatRole.NOTIFICATION) {
				return messages.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns an unmodifiable snapshot of all messages.
	 *
	 * @return an unmodifiable snapshot list of messages
	 */
	public synchronized Iterable<ChatMessage> getMessages() {
		// Unmodifiable snapshot to avoid concurrent modification after returning
		return Collections.unmodifiableList(new ArrayList<>(messages));
	}

	/**
	 * Returns an iterable of messages, excluding the last message if it has no content.
	 * This is primarily used for API calls where empty messages should be excluded.
	 *
	 * @return an iterable of the messages suitable for API transmission
	 */
	public synchronized Iterable<ChatMessage> getMessagesExcludingLastIfEmpty() {
		if (!messages.isEmpty() && messages.peek().getContent().isEmpty()) {
			// Snapshot excluding the last (empty) message
			return Collections.unmodifiableList(new ArrayList<>(messages.subList(0, messages.size() - 1)));
		}
		// Full snapshot
		return Collections.unmodifiableList(new ArrayList<>(messages));
	}

	/**
	 * Converts the conversation's full state (messages and redo history) to JSON.
	 * This preserves complete conversation state for persistence across sessions.
	 *
	 * @return a JSON string containing the conversation's complete state
	 * @throws IOException if the JSON serialization fails
	 */
	public synchronized String toJson() throws IOException {
		Map<String, Object> data = new HashMap<>();
		data.put("messages", messages);
		data.put("redoStack", redoStack);
		return objectMapper.writeValueAsString(data);
	}

	/**
	 * Creates a ChatConversation from a JSON string containing full conversation state.
	 * Restores both message history and redo stack for complete state restoration.
	 *
	 * @param json the JSON string containing "messages" and "redoStack" fields
	 * @return a new ChatConversation with complete state from the JSON
	 * @throws IOException if the JSON cannot be parsed or the expected structure is missing
	 */
	public static ChatConversation fromJson(String json) throws IOException {
		JsonNode root = objectMapper.readTree(json);
		ChatConversation conversation = new ChatConversation();

		JavaType stackType =
				objectMapper.getTypeFactory().constructCollectionType(Stack.class, ChatMessage.class);
		Stack<ChatMessage> messageStack =
				objectMapper.convertValue(root.get("messages"), stackType);
		conversation.messages.addAll(messageStack);

		JavaType redoStackType =
				objectMapper.getTypeFactory().constructCollectionType(
						Stack.class,
						objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
		Stack<List<ChatMessage>> redoStackData =
				objectMapper.convertValue(root.get("redoStack"), redoStackType);
		conversation.redoStack.addAll(redoStackData);

		return conversation;
	}

	/**
	 * Exports a transcript view to Markdown. Filters to USER and ASSISTANT roles only,
	 * skipping NOTIFICATION messages and any messages with empty content.
	 * Each message is prefixed with "### ROLE:" followed by the content. Messages are separated by a blank line.
	 *
	 * @return a markdown-formatted transcript string
	 */
	public synchronized String toMarkdown() {
		StringBuilder markdown = new StringBuilder();
		for (ChatMessage message : messages) {
			if (message.getRole() == ChatRole.USER || message.getRole() == ChatRole.ASSISTANT) {
				if (!message.getContent().trim().isEmpty()) {
					markdown.append("### ").append(message.getRole()).append(":\n\n")
					.append(message.getContent()).append("\n\n");
				}
			}
		}
		return markdown.toString().trim();
	}

}