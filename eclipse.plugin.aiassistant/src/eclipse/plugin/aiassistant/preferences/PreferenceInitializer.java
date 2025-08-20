package eclipse.plugin.aiassistant.preferences;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.prompt.PromptLoader;
import eclipse.plugin.aiassistant.prompt.Prompts;

/**
 * Class responsible for initializing default preference values for the AI Assistant plugin.
 * This includes setting up API configurations, UI settings, and loading default prompts.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * Initializes all default preference settings required by the AI Assistant.
	 * This method is called when the application starts to ensure all necessary defaults are set.
	 */
	@Override
	public void initializeDefaultPreferences() {
		setDefaultConnectionTimeout();
		setDefaultRequestTimeout();
		setDefaultStreamingUpdateInterval();
		setDefaultChatFontSize();
		setDefaultNotificationFontSize();
		setDefaultDisableTooltips();
		setDefaultCurrentModelName();
		setDefaultCurrentApiUrl();
		setDefaultCurrentApiKey();
		setDefaultCurrentJsonOverrides();
		setDefaultCurrentUseStreaming();
		setDefaultCurrentUseSystemMessage();
		setDefaultCurrentUseDeveloperMessage();
		setDefaultBookmarkedApiSettings();
		setDefaultChatConversations();
		setDefaultUserMessageHistory();
		loadAndSetDefaultPrompts();
	}

	/**
	 * Sets the default connection timeout for API requests.
	 */
	private void setDefaultConnectionTimeout() {
		Preferences.getDefault().setDefault(PreferenceConstants.CONNECTION_TIMEOUT, Constants.DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * Sets the default request timeout for API requests.
	 */
	private void setDefaultRequestTimeout() {
		Preferences.getDefault().setDefault(PreferenceConstants.REQUEST_TIMEOUT, Constants.DEFAULT_REQUEST_TIMEOUT);
	}

	private void setDefaultStreamingUpdateInterval() {
		Preferences.getDefault().setDefault(PreferenceConstants.STREAMING_UPDATE_INTERVAL, Constants.DEFAULT_STREAMING_UPDATE_INTERVAL);
	}

	/**
	 * Sets the default font size for chat interface elements.
	 */
	private void setDefaultChatFontSize() {
		Preferences.getDefault().setDefault(PreferenceConstants.CHAT_FONT_SIZE, Constants.DEFAULT_CHAT_FONT_SIZE);
	}

	/**
	 * Sets the default font size for notification elements.
	 */
	private void setDefaultNotificationFontSize() {
		Preferences.getDefault().setDefault(PreferenceConstants.NOTIFICATION_FONT_SIZE, Constants.DEFAULT_NOTIFICATION_FONT_SIZE);
	}

	/**
	 * Sets whether tooltips should be disabled across the application.
	 */
	private void setDefaultDisableTooltips() {
		Preferences.getDefault().setDefault(PreferenceConstants.DISABLE_TOOLTIPS, Constants.DEFAULT_DISABLE_TOOLTIPS);
	}

	/**
	 * Sets the default currently selected model name in the preference store.
	 */
	private void setDefaultCurrentModelName() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_MODEL_NAME, Constants.DEFAULT_MODEL_NAME);
	}

	/**
	 * Sets the default currently selected API URL in the preference store.
	 */
	private void setDefaultCurrentApiUrl() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_API_URL, Constants.DEFAULT_API_URL);
	}

	/**
	 * Sets the default currently selected API key fin the preference store.
	 */
	private void setDefaultCurrentApiKey() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_API_KEY, Constants.DEFAULT_API_KEY);
	}

	/**
	 * Sets the default JSON overrides setting in the preference store.
	 */
	private void setDefaultCurrentJsonOverrides() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_JSON_OVERRIDES,
				Constants.DEFAULT_JSON_OVERRIDES);
	}

	/**
	 * Sets the default currently selected streaming flag setting in the preference store.
	 */
	private void setDefaultCurrentUseStreaming() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_USE_STREAMING, Constants.DEFAULT_USE_STREAMING);
	}

	/**
	 * Sets the default currently selected system message flag setting in the preference store.
	 */
	private void setDefaultCurrentUseSystemMessage() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_USE_SYSTEM_MESSAGE,
				Constants.DEFAULT_USE_SYSTEM_MESSAGE);
	}

	/**
	 * Sets the default currently selected developer message flag setting in the preference store.
	 */
	private void setDefaultCurrentUseDeveloperMessage() {
		Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_USE_DEVELOPER_MESSAGE,
				Constants.DEFAULT_USE_DEVELOPER_MESSAGE);
	}

	/**
	 * Sets the default bookmarked API settings to a few useful examples.
	 */
	private void setDefaultBookmarkedApiSettings() {
		try {
			String serializedSettings = Preferences.serializeBookmarkedApiSettings(Constants.DEFAULT_BOOKMARKED_API_SETTINGS);
			Preferences.getDefault().setDefault(PreferenceConstants.BOOKMARKED_API_SETTINGS, serializedSettings);
		} catch (IOException e) {
			Logger.warning("Failed to set default bookmarked API settings: " + e.getMessage());
		}
	}

	/**
	 * Sets the default value for the chat conversations in the preferences.
	 * This method initializes the chat conversations setting to an empty string,
	 * which might be used to represent no initial conversation history.
	 *
	 * @see Preferences#setDefault(String, String)
	 */
	private void setDefaultChatConversations() {
		Preferences.getDefault().setDefault(PreferenceConstants.CHAT_CONVERSATIONS, "");
	}

	/**
	 * Sets the default value for the user message history in the preferences.
	 * This method initializes the user message history to an empty string,
	 * indicating that there is no previous user message history by default.
	 *
	 * @see Preferences#setDefault(String, String)
	 */
	private void setDefaultUserMessageHistory() {
		Preferences.getDefault().setDefault(PreferenceConstants.USER_MESSAGE_HISTORY, "");
	}

	/**
	 * Loads default prompts from files and sets them in the preference store.
	 * Each prompt type defined in {@link Prompts} is loaded and stored.
	 */
	private void loadAndSetDefaultPrompts() {
		for (Prompts prompt : Prompts.values()) {
			Preferences.getDefault().setDefault(prompt.preferenceName(), PromptLoader.getRawPrompt(prompt.getFileName()));
		}
	}

}