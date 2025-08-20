package eclipse.plugin.aiassistant.preferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

import eclipse.plugin.aiassistant.Activator;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.view.UserMessageHistory;

/**
 * This class provides utility methods for accessing and managing preferences in
 * the application.
 */
public final class Preferences {

	private static final String PREFERENCE_PAGE_ID = "eclipse.plugin.aiassistant.preferences.PreferencePage";
	private static final String PROMPT_TEMPLATES_PAGE_ID = "eclipse.plugin.aiassistant.preferences.PromptTemplatesPreferencePage";

	private static final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private Preferences() {
	}

	/**
	 * Opens the preference dialog for the AssistAI plugin.
	 */
	public static void openPreferenceDialog() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(Eclipse.getShell(), PREFERENCE_PAGE_ID,
				new String[] { PREFERENCE_PAGE_ID, PROMPT_TEMPLATES_PAGE_ID }, null);
		dialog.open();
	}

	/**
	 * Returns the default preference store.
	 *
	 * @return The default preference store.
	 */
	public static IPreferenceStore getDefault() {
		return preferenceStore;
	}

	/**
	 * Returns the connection timeout value.
	 *
	 * @return The connection timeout value.
	 */
	public static Integer getConnectionTimeout() {
		return preferenceStore.getInt(PreferenceConstants.CONNECTION_TIMEOUT);
	}

	/**
	 * Returns the request timeout value.
	 *
	 * @return The request timeout value.
	 */
	public static Integer getRequestTimeout() {
		return preferenceStore.getInt(PreferenceConstants.REQUEST_TIMEOUT);
	}

	public static Integer getStreamingUpdateInterval() {
		return preferenceStore.getInt(PreferenceConstants.STREAMING_UPDATE_INTERVAL);
	}

	/**
	 * Returns the chat font size.
	 *
	 * @return The chat font size.
	 */
	public static Integer getChatFontSize() {
		return preferenceStore.getInt(PreferenceConstants.CHAT_FONT_SIZE);
	}

	/**
	 * Returns the notification font size.
	 *
	 * @return The notification font size.
	 */
	public static Integer getNotificationFontSize() {
		return preferenceStore.getInt(PreferenceConstants.NOTIFICATION_FONT_SIZE);
	}

	/**
	 * This method retrieves a boolean value from the preference store indicating
	 * whether to disable tooltips.
	 *
	 * @return A boolean value indicating whether to disable tooltips.
	 */
	public static boolean disableTooltips() {
		return preferenceStore.getBoolean(PreferenceConstants.DISABLE_TOOLTIPS);
	}

	/**
	 * Returns the currently selected model name from the preference store.
	 *
	 * @return The currently selected model name.
	 */
	public static String getCurrentModelName() {
		return preferenceStore.getString(PreferenceConstants.CURRENT_MODEL_NAME);
	}

	/**
	 * Retrieves the currently selected API URL from the preference store.
	 *
	 * @return The base URL of the API as a {@link URL} object.
	 */
	public static String getCurrentApiUrl() {
		return preferenceStore.getString(PreferenceConstants.CURRENT_API_URL);
	}

	/**
	 * Retrieves the currently selected API key from the preference store.
	 *
	 * @return The API key as a {@link String}.
	 */
	public static String getCurrentApiKey() {
		return preferenceStore.getString(PreferenceConstants.CURRENT_API_KEY);
	}

	/**
	 * Returns the currently selected JSON overrides from the preference store.
	 *
	 * @return The JSON overrides string.
	 */
	public static String getCurrentJsonOverrides() {
		return preferenceStore.getString(PreferenceConstants.CURRENT_JSON_OVERRIDES);
	}

	/**
	 * Returns the currently selected streaming flag from the preference store.
	 *
	 * @return The streaming flag value.
	 */
	public static Boolean getCurrentUseStreaming() {
		return preferenceStore.getBoolean(PreferenceConstants.CURRENT_USE_STREAMING);
	}

	/**
	 * Returns the currently selected system message flag from the preference store.
	 *
	 * @return The message flag value.
	 */
	public static Boolean getCurrentUseSystemMessage() {
		return preferenceStore.getBoolean(PreferenceConstants.CURRENT_USE_SYSTEM_MESSAGE);
	}

	/**
	 * Returns the currently selected developer message flag from the preference store.
	 *
	 * @return The developer message flag value.
	 */
	public static Boolean getCurrentUseDeveloperMessage() {
		return preferenceStore.getBoolean(PreferenceConstants.CURRENT_USE_DEVELOPER_MESSAGE);
	}

	/**
	 * Serializes a list of BookmarkedApiSettings to a Base64 encoded string.
	 *
	 * @param bookmarkedApiSettings The list of settings to serialize
	 * @return Base64 encoded string representation
	 * @throws IOException If serialization fails
	 */
	public static String serializeBookmarkedApiSettings(List<BookmarkedApiSettings> bookmarkedApiSettings) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(bookmarkedApiSettings);
		}
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	/**
	 * Deserializes a Base64 encoded string back to a list of BookmarkedApiSettings.
	 *
	 * @param serializedData Base64 encoded string to deserialize
	 * @return The deserialized list of BookmarkedApiSettings
	 * @throws IOException If deserialization fails
	 * @throws ClassNotFoundException If the BookmarkedApiSettings class cannot be found
	 */
	@SuppressWarnings("unchecked")
	public static List<BookmarkedApiSettings> deserializeBookmarkedApiSettings(String serializedData)
			throws IOException, ClassNotFoundException {
		if (serializedData == null || serializedData.isEmpty()) {
			return new ArrayList<>();
		}

		byte[] data = Base64.getDecoder().decode(serializedData);
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
			return (List<BookmarkedApiSettings>) ois.readObject();
		}
	}

	/**
	 * Saves the current state of the bookmarked API settings to the preference store.
	 *
	 * @param bookmarkedApiSettings The list of settings to be saved.
	 * @throws IOException If an error occurs during the serialization process.
	 */
	public static void saveBookmarkedApiSettings(List<BookmarkedApiSettings> bookmarkedApiSettings) throws IOException {
		String serializedData = serializeBookmarkedApiSettings(bookmarkedApiSettings);
		preferenceStore.setValue(PreferenceConstants.BOOKMARKED_API_SETTINGS, serializedData);
	}

	/**
	 * Loads a set of bookmarked API settings from the preference store.
	 *
	 * @return The deserialized list of BookmarkedApiSettings.
	 * @throws IOException If an error occurs during the deserialization process.
	 * @throws ClassNotFoundException If the class of a serialized object cannot be found.
	 */
	public static List<BookmarkedApiSettings> loadBookmarkedApiSettings() throws IOException, ClassNotFoundException {
		String serializedData = preferenceStore.getString(PreferenceConstants.BOOKMARKED_API_SETTINGS);
		return deserializeBookmarkedApiSettings(serializedData);
	}

	/**
	 * Saves the current state of multiple ChatConversations to the preference store.
	 *
	 * @param conversations The list of ChatConversation objects to be saved.
	 * @throws IOException If an error occurs during the serialization process.
	 */
	public static void saveChatConversations(List<ChatConversation> conversations) throws IOException {
		// Create wrapper object for conversations
		ChatConversationsWrapper wrapper = new ChatConversationsWrapper(conversations);
		String serializedData = wrapper.serialize();
		preferenceStore.setValue(PreferenceConstants.CHAT_CONVERSATIONS, serializedData);
	}

	/**
	 * Loads multiple ChatConversations from the preference store.
	 *
	 * @return The list of deserialized ChatConversation objects.
	 * @throws IOException If an error occurs during the deserialization process.
	 */
	public static List<ChatConversation> loadChatConversations() throws IOException {
		String serializedData = preferenceStore.getString(PreferenceConstants.CHAT_CONVERSATIONS);
		if (serializedData == null || serializedData.trim().isEmpty()) {
			// Return single empty conversation as default
			List<ChatConversation> defaultConversations = new ArrayList<>();
			defaultConversations.add(new ChatConversation());
			return defaultConversations;
		}
		ChatConversationsWrapper wrapper = ChatConversationsWrapper.deserialize(serializedData);
		return wrapper.getConversations();
	}

	/**
	 * Saves the current state of a UserMessageHistory to the preference store.
	 *
	 * @param history The UserMessageHistory object to be saved.
	 * @throws IOException If an error occurs during the serialization process.
	 */
	public static void saveUserMessageHistory(UserMessageHistory history) throws IOException {
		String serializedData = history.serialize();
		preferenceStore.setValue(PreferenceConstants.USER_MESSAGE_HISTORY, serializedData);
	}

	/**
	 * Loads a UserMessageHistory from the preference store.
	 *
	 * @return The deserialized UserMessageHistory object.
	 * @throws IOException If an error occurs during the deserialization process.
	 */
	public static UserMessageHistory loadUserMessageHistory() throws IOException {
		String serializedData = preferenceStore.getString(PreferenceConstants.USER_MESSAGE_HISTORY);
		return UserMessageHistory.deserialize(serializedData);
	}

	/**
	 * Internal wrapper class for serializing/deserializing multiple ChatConversations.
	 */
	private static class ChatConversationsWrapper {
		private List<ChatConversation> conversations;

		public ChatConversationsWrapper(List<ChatConversation> conversations) {
			this.conversations = conversations;
		}

		public List<ChatConversation> getConversations() {
			return conversations;
		}

		public String serialize() throws IOException {
			StringBuilder json = new StringBuilder();
			json.append("{\"conversations\":[");
			for (int i = 0; i < conversations.size(); i++) {
				if (i > 0) json.append(",");
				json.append(conversations.get(i).serialize());
			}
			json.append("]}");
			return json.toString();
		}

		public static ChatConversationsWrapper deserialize(String json) throws IOException {
			// Simple JSON parsing - extract conversations array
			List<ChatConversation> conversations = new ArrayList<>();

			// Find the conversations array
			int startIdx = json.indexOf("\"conversations\":[") + 17;
			int endIdx = json.lastIndexOf("]}");

			if (startIdx < 17 || endIdx < 0) {
				// Fallback for invalid format
				conversations.add(new ChatConversation());
				return new ChatConversationsWrapper(conversations);
			}

			String conversationsJson = json.substring(startIdx, endIdx);
			if (conversationsJson.trim().isEmpty()) {
				conversations.add(new ChatConversation());
				return new ChatConversationsWrapper(conversations);
			}

			// Split conversations by finding complete JSON objects
			List<String> conversationJsons = splitConversationJsons(conversationsJson);

			for (String conversationJson : conversationJsons) {
				try {
					conversations.add(ChatConversation.deserialize(conversationJson.trim()));
				} catch (IOException e) {
					// Skip invalid conversations, continue with others
				}
			}

			// Ensure at least one conversation
			if (conversations.isEmpty()) {
				conversations.add(new ChatConversation());
			}

			return new ChatConversationsWrapper(conversations);
		}

		private static List<String> splitConversationJsons(String conversationsJson) {
			List<String> result = new ArrayList<>();
			int braceLevel = 0;
			int startIdx = 0;

			for (int i = 0; i < conversationsJson.length(); i++) {
				char c = conversationsJson.charAt(i);
				if (c == '{') {
					braceLevel++;
				} else if (c == '}') {
					braceLevel--;
					if (braceLevel == 0) {
						result.add(conversationsJson.substring(startIdx, i + 1));
						// Skip comma and whitespace
						i++;
						while (i < conversationsJson.length() &&
								(conversationsJson.charAt(i) == ',' || Character.isWhitespace(conversationsJson.charAt(i)))) {
							i++;
						}
						startIdx = i;
						i--; // Adjust for loop increment
					}
				}
			}

			return result;
		}
	}

}