package eclipse.plugin.aiassistant.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UserMessageHistory class is responsible for storing and retrieving user
 * messages. It provides methods to store a message, get the previous message,
 * and get the next message.
 */
public class UserMessageHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<String> storedMessages = new ArrayList<>();
	private int currentIndex = 0;

	/**
	 * Replaces this history's internal state with another history's state.
	 * Clears current stored messages, then copies both messages and current index from 'other'.
	 */
	public synchronized void copyFrom(UserMessageHistory other) {
		storedMessages.clear();
		for (String message : other.storedMessages) {
			storedMessages.add(message);
		}
		currentIndex = other.currentIndex;
	}

	/**
	 * Checks if the message history is empty.
	 *
	 * @return true if no messages are stored, false otherwise.
	 */
	public boolean isEmpty() {
		return storedMessages.isEmpty();
	}

	/**
	 * Returns the number of stored messages.
	 *
	 * @return the size of the message history.
	 */
	public int size() {
		return storedMessages.size();
	}

	/**
	 * Checks if there are older messages available without changing position.
	 *
	 * @return true if there are older messages, false otherwise.
	 */
	public boolean hasOlderMessages() {
		return currentIndex > 0;
	}

	/**
	 * Checks if there are newer messages available without changing position.
	 *
	 * @return true if there are newer messages, false otherwise.
	 */
	public boolean hasNewerMessages() {
		return currentIndex < storedMessages.size();
	}

	/**
	 * Stores a message if it is not empty. If the message already exists in the
	 * storedMessages list, it is moved to the end (most recent position) rather
	 * than creating a duplicate entry. This ensures recent messages appear at
	 * the end of history navigation.
	 * Updates the currentIndex to point to the last element in the list.
	 *
	 * @param message The message to be stored.
	 */
	public void storeMessage(String message) {
		if (!message.trim().isEmpty()) {
			// Remove existing occurrence if present, then add to end
			if (storedMessages.contains(message)) {
				storedMessages.remove(message);
			}
			storedMessages.add(message);
		}
		currentIndex = storedMessages.size();
	}

	/**
	 * Retrieves the older message from the storedMessages list if it exists.
	 * Decrements the currentIndex by 1 and returns the message at that index.
	 *
	 * @return The older message if it exists, null otherwise.
	 */
	public String getOlderMessage() {
		if (currentIndex > 0) {
			currentIndex--;
			return storedMessages.get(currentIndex);
		}
		return null;
	}

	/**
	 * Retrieves the newer message from the storedMessages list if it exists.
	 * Increments the currentIndex by 1 and returns the message at that index.
	 *
	 * @return The newer message if it exists, null otherwise.
	 */
	public String getNewerMessage() {
		if (currentIndex < storedMessages.size()) {
			currentIndex++;
			if (currentIndex < storedMessages.size()) {
				return storedMessages.get(currentIndex);
			}
		}
		return null;
	}

	/**
	 * Resets the current position to the newest message (end of history).
	 * This is equivalent to what happens after storing a new message.
	 */
	public void resetPosition() {
		currentIndex = storedMessages.size();
	}

	/**
	 * Clears all stored messages and resets the current position.
	 */
	public void clear() {
		storedMessages.clear();
		currentIndex = 0;
	}

}