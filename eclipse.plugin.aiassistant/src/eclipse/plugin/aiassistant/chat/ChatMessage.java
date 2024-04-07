package eclipse.plugin.aiassistant.chat;

import java.util.UUID;

/**
 * The ChatMessage class represents a message in a chat conversation.
 */
public class ChatMessage {

    private final UUID id;
    private final ChatRole role;
    private StringBuilder message;

    /**
     * Constructs a new ChatMessage with the specified role.
     * 
     * @param role the role of the user who sent this message
     */
    public ChatMessage(ChatRole role) {
        this(role, "");
    }

    /**
     * Constructs a new ChatMessage with the specified role and message content.
     * 
     * @param role    the role of the user who sent this message
     * @param message the content of the message
     */
    public ChatMessage(ChatRole role, String message) {
        this.id = UUID.randomUUID();
        this.role = role;
        this.message = new StringBuilder(message);
    }

    /**
     * Returns the unique identifier for this message.
     * 
     * @return the unique identifier for this message
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the role of the user who sent this message.
     * 
     * @return the role of the user who sent this message
     */
    public ChatRole getRole() {
        return role;
    }

    /**
     * Returns the content of the message.
     * 
     * @return the content of the message
     */
    public String getMessage() {
        return message.toString();
    }

    /**
     * Sets the content of the message to the specified string.
     * 
     * @param message the new content of the message
     */
    public void setMessage(String message) {
        this.message = new StringBuilder(message);
    }

    /**
     * Appends the specified string to the end of the message content.
     * 
     * @param message the string to append to the message content
     */
    public void appendMessage(String message) {
        this.message.append(message);
    }
}