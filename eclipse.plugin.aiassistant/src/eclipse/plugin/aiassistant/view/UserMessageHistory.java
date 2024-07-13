package eclipse.plugin.aiassistant.view;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The UserMessageHistory class is responsible for storing and retrieving user
 * messages. It provides methods to store a message, get the previous message,
 * and get the next message.
 */
public class UserMessageHistory {

	// Maximum number of messages to serialize
    private static final int MAX_SERIALIZED_MESSAGES = 100;

	private static final ObjectMapper objectMapper = new ObjectMapper();
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
	
    /**
     * Serializes the UserMessageHistory to a JSON string.
     *
     * @return a JSON string representing the stored messages
     * @throws IOException if an input/output exception occurs
     */
	public synchronized String serialize() throws IOException {
		List<String> messagesToSerialize;
		if (storedMessages.size() > MAX_SERIALIZED_MESSAGES) {
			messagesToSerialize = storedMessages.subList(storedMessages.size() - MAX_SERIALIZED_MESSAGES,
					storedMessages.size());
		} else {
			messagesToSerialize = new ArrayList<>(storedMessages);
		}
		return objectMapper.writeValueAsString(messagesToSerialize);
	}

    /**
     * Deserializes a JSON string to a UserMessageHistory object.
     * Handles empty or null JSON input by returning an empty UserMessageHistory instance.
     *
     * @param json the JSON string representing the stored messages
     * @return a UserMessageHistory object
     * @throws IOException if an input/output exception occurs
     */
    public static UserMessageHistory deserialize(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return new UserMessageHistory();  // Return an empty history if input is empty or null
        }
        List<String> messages = objectMapper.readValue(json, new TypeReference<List<String>>() {});
        UserMessageHistory history = new UserMessageHistory();
        history.storedMessages.addAll(messages);
        history.currentIndex = messages.size(); // Set currentIndex to point to the last message
        return history;
    }

}