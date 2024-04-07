package eclipse.plugin.aiassistant.chat;

/**
 * The Role enum represents the role in a chat conversation.
 */
public enum ChatRole {
	
	USER("user"), 
	ASSISTANT("assistant"), 
	NOTIFICATION("notification");

	private final String roleName;

	private ChatRole(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * Returns the name of the role as a string.
	 * 
	 * @return the name of the role as a string.
	 */
	public String getRoleName() {
		return this.roleName;
	}
}