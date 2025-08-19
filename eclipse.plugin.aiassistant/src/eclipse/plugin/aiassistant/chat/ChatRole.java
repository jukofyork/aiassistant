package eclipse.plugin.aiassistant.chat;

/**
 * The ChatRole enum represents the role in a chat conversation.
 */
public enum ChatRole {
	USER,
	ASSISTANT,
	NOTIFICATION;

	@Override
	public String toString() {
		return name();
	}
}