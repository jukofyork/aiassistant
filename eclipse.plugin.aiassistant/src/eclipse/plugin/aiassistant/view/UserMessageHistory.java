package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;

/**
 * The UserMessageHistory class is responsible for storing and retrieving user
 * messages. It provides methods to store a message, get the previous message,
 * and get the next message.
 */
public class UserMessageHistory {

	private List<String> storedMessages = new ArrayList<>();
	private int currentIndex = 0;

	/**
	 * Stores a message if it is not empty and not already present in the
	 * storedMessages list. Updates the currentIndex to point to the last element in
	 * the list.
	 * 
	 * @param message The message to be stored.
	 */
	public void storeMessage(String message) {
		if (!message.trim().isEmpty() && !storedMessages.contains(message)) {
			storedMessages.add(message);
		}
		currentIndex = storedMessages.size();
	}

	/**
	 * Retrieves the previous message from the storedMessages list if it exists.
	 * Decrements the currentIndex by 1 and returns the message at that index.
	 * 
	 * @return The previous message if it exists, null otherwise.
	 */
	public String getPreviousMessage() {
		if (currentIndex > 0) {
			currentIndex--;
			return storedMessages.get(currentIndex);
		}
		return null;
	}

	/**
	 * Retrieves the next message from the storedMessages list if it exists.
	 * Increments the currentIndex by 1 and returns the message at that index.
	 * 
	 * @return The next message if it exists, null otherwise.
	 */
	public String getNextMessage() {
		if (currentIndex < storedMessages.size()) {
			currentIndex++;
			if (currentIndex < storedMessages.size()) {
				return storedMessages.get(currentIndex);
			}
		}
		return null;
	}

}