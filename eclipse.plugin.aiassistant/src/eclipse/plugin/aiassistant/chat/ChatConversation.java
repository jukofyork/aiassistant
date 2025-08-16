package eclipse.plugin.aiassistant.chat;

import java.io.IOException;
import java.util.Collections;
import java.util.Stack;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents a chat conversation.
 */
public class ChatConversation {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final Stack<ChatMessage> messages = new Stack<>();

	/**
	 * Adds a message to the conversation.
	 *
	 * @param message the message to add
	 */
	public synchronized void push(ChatMessage message) {
		messages.push(message);
	}

	/**
	 * Checks if the conversation has no messages.
	 *
	 * @return true if the conversation is empty, false otherwise
	 */
	public boolean isEmpty() {
		return messages.isEmpty();
	}

	/**
	 * Returns the number of messages in the conversation.
	 *
	 * @return the number of messages in the conversation
	 */
	public int size() {
		return messages.size();
	}

	/**
	 * Checks if a message with the given UUID exists in the conversation.
	 *
	 * @param id the UUID of the message to check
	 * @return true if the message exists, false otherwise
	 */
	public boolean contains(UUID id) {
		for (ChatMessage message : messages) {
			if (message.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the first message in the conversation that is not a notification.
	 *
	 *  @return the first message in the conversation, or null if there are no
	 *         messages
	 */
	public ChatMessage getFirstMessage() {
		for (ChatMessage message : messages) {
			if (message.getRole() != ChatRole.NOTIFICATION) {
				return message;
			}
		}
		return null;
	}

	/**
	 * Returns the last message in the conversation that is not a notification.
	 *
	 *  @return the last message in the conversation, or null if there are no
	 *         messages
	 */
	public ChatMessage getLastMessage() {
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i).getRole() != ChatRole.NOTIFICATION) {
				return messages.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns the message with the next lowest index in the conversation that is not a notification.
	 *
	 *  @param id the UUID of the message to find
	 *  @return the next lowest message in the conversation, or null if there is no
	 *         such message or no previous message
	 */
	public ChatMessage getPreviousMessage(UUID id) {
		int index = -1;
		for (int i = 0; i < messages.size(); i++) {
			if (messages.get(i).getId().equals(id)) {
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
	 * Returns the message with the next highest index in the conversation that is not a notification.
	 *
	 *  @param id the UUID of the message to find
	 *  @return the next highest message in the conversation, or null if there is no
	 *         such message or no next message
	 */
	public ChatMessage getNextMessage(UUID id) {
		int index = -1;
		for (int i = 0; i < messages.size(); i++) {
			if (messages.get(i).getId().equals(id)) {
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
	 * Returns an iterable of all messages in the conversation.
	 *
	 * @return an iterable of all messages in the conversation
	 */
	public Iterable<ChatMessage> messages() {
		return messages;
	}

	/**
	 * Returns an iterable of messages, skipping the last message if it has no content.
	 * This is primarily used for API calls where empty messages should be excluded.
	 *
	 * @return an iterable of the messages suitable for API transmission
	 */
	public Iterable<ChatMessage> messagesExcludingLastIfEmpty() {
		if (!messages.isEmpty() &&
				(messages.peek().getMessage() == null || messages.peek().getMessage().isEmpty())) {
			return messages.subList(0, messages.size() - 1);
		}
		return messages;
	}

	/**
	 * This method undoes the last "conversation interaction". It removes messages
	 * from the stack until it encounters a user message or when the stack is empty.
	 *
	 * @return A ChatConversation containing the removed messages in their original order.
	 */
	public ChatConversation undo() {
		ChatConversation removedConversation = new ChatConversation();
		while (!messages.isEmpty()) {
			ChatMessage message = messages.pop();
			removedConversation.push(message);
			if (message.getRole() == ChatRole.USER) {
				break;
			}
		}
		Collections.reverse(removedConversation.messages);
		return removedConversation;
	}

	/**
	 * Clears the conversation and returns the cleared messages.
	 *
	 * @return A ChatConversation containing the cleared messages.
	 */
	public ChatConversation clear() {
		ChatConversation clearedConversation = new ChatConversation();
		clearedConversation.messages.addAll(messages);
		messages.clear();
		return clearedConversation;
	}

	/**
	 * This method scans backwards through the stack and returns true if it finds an
	 * USER message, but returns false as soon as it finds an ASSISTANT message or
	 * reaches the beginning finding a USER message.
	 *
	 * @return true if it finds an USER message, false otherwise
	 */
	public boolean hasUnsentUserMessages() {
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
	 * Returns the total character count of all message content in the conversation.
	 * This can be used as a heuristic for determining UI optimization needs.
	 *
	 * @return the total number of characters across all messages
	 */
	public int getContentSize() {
		int totalSize = 0;
		for (ChatMessage message : messages) {
			String messageText = message.getMessage();
			if (messageText != null) {
				totalSize += messageText.length();
			}
		}
		return totalSize;
	}

	/**
	 * Converts the conversation to a markdown-formatted string.
	 * User messages are prefixed with "### USER:" and assistant messages with "### ASSISTANT:".
	 * Notification messages are skipped.
	 *
	 * @return a markdown-formatted string representation of the conversation
	 */
	public String toMarkdown() {
		StringBuilder markdown = new StringBuilder();
		boolean first = true;

		for (ChatMessage message : messages) {
			String content = message.getMessage();
			if (content == null || content.trim().isEmpty()) {
				continue; // Skip empty messages
			}

			switch (message.getRole()) {
			case USER:
				if (!first) {
					markdown.append("\n\n");
				}
				markdown.append("### USER:\n\n");
				markdown.append(content);
				first = false;
				break;
			case ASSISTANT:
				if (!first) {
					markdown.append("\n\n");
				}
				markdown.append("### ASSISTANT:\n\n");
				markdown.append(content);
				first = false;
				break;
			case NOTIFICATION:
				// Skip notification messages as they're internal to the application
				break;
			}
		}

		return markdown.toString();
	}

	/**
	 * Serializes this ChatConversation to a JSON string.
	 *
	 * @return a JSON string representing this conversation
	 * @throws IOException if an input/output exception occurs
	 */
	public synchronized String serialize() throws IOException {
		return objectMapper.writeValueAsString(messages);
	}

	/**
	 * Deserializes a JSON string to a ChatConversation.
	 *
	 * @param json the JSON string representing a conversation
	 * @return a ChatConversation object
	 * @throws IOException if an input/output exception occurs
	 */
	public static ChatConversation deserialize(String json) throws IOException {
		Stack<ChatMessage> messageStack = objectMapper.readValue(json, new TypeReference<Stack<ChatMessage>>() {});
		ChatConversation conversation = new ChatConversation();
		conversation.messages.addAll(messageStack);
		return conversation;
	}

}