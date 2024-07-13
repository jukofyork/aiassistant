package eclipse.plugin.aiassistant.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

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
	 * Returns an iterable of the messages in the conversation.
	 *
	 * @return an iterable of the messages in the conversation
	 */
	public Iterable<ChatMessage> messages() {
		return messages;
	}

	/**
	 * This method undoes the last "conversation interaction". It removes messages
	 * from the stack until it encounters a user message or when the stack is empty.
	 *
	 * @return A list of the IDs of the removed messages.
	 */
	public List<UUID> undo() {
		List<UUID> removedIds = new ArrayList<>();
		while (!messages.isEmpty()) {
			ChatMessage message = messages.pop();
			removedIds.add(message.getId());
			if (message.getRole() == ChatRole.USER) {
				break;
			}
		}
		return removedIds;
	}

	/**
	 * Clears the conversation.
	 */
	public void clear() {
		messages.clear();
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
