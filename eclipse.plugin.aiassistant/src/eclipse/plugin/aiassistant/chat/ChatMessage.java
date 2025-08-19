package eclipse.plugin.aiassistant.chat;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The ChatMessage class represents a content in a chat conversation.
 */
public class ChatMessage {

	private final UUID id;
	private final ChatRole role;
	private StringBuilder content;

	/**
	 * JsonCreator constructor for deserialization with Jackson.
	 */
	@JsonCreator
	public ChatMessage(
			@JsonProperty("id") UUID id,
			@JsonProperty("role") ChatRole role,
			@JsonProperty("content") String content) {
		this.id = id;
		this.role = role;
		this.content = new StringBuilder(content);
	}

	/**
	 * Constructs a new ChatMessage with the specified role.
	 *
	 * @param role the role of the user who sent this content
	 */
	public ChatMessage(ChatRole role) {
		this(role, "");
	}

	/**
	 * Constructs a new ChatMessage with the specified role and content content.
	 *
	 * @param role    the role of the user who sent this content
	 * @param content the content of the content
	 */
	public ChatMessage(ChatRole role, String content) {
		this.id = UUID.randomUUID();
		this.role = role;
		this.content = new StringBuilder(content);
	}

	/**
	 * Returns the unique identifier for this content.
	 *
	 * @return the unique identifier for this content
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Returns the role of the user who sent this content.
	 *
	 * @return the role of the user who sent this content
	 */
	public ChatRole getRole() {
		return role;
	}

	/**
	 * Returns the content of the content.
	 *
	 * @return the content of the content
	 */
	public synchronized String getContent() {
		return content.toString();
	}

	/**
	 * Sets the content of the content to the specified string.
	 *
	 * @param content the new content of the content
	 */
	public synchronized void setContent(String content) {
		this.content = new StringBuilder(content);
	}

	/**
	 * Appends the specified string to the end of the content content.
	 *
	 * @param content the string to append to the content content
	 */
	public synchronized void appendContent(String content) {
		this.content.append(content);
	}
}