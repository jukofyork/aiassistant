package eclipse.plugin.aiassistant.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        setDefaultChatFontSize();
        setDefaultNotificationFontSize();
        setDefaultUseStreaming();
        setDefaultDisableTooltips();
        setDefaultCurrentModelName();
        setDefaultCurrentApiUrl();
        setDefaultCurrentApiKey();
        setDefaultCurrentTemperature();
        setDefaultBookmarkedApiSettings();
        setDefaultChatConversation();
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
     * Sets the default value for using streaming.
     */
    private void setDefaultUseStreaming() {
        Preferences.getDefault().setDefault(PreferenceConstants.USE_STREAMING, Constants.DEFAULT_USE_STREAMING);
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
     * Sets the default currently selected temperature setting in the preference store.
     */
    private void setDefaultCurrentTemperature() {
        Preferences.getDefault().setDefault(PreferenceConstants.CURRENT_TEMPERATURE, Constants.DEFAULT_TEMPERATURE);
    }

    /**
     * Sets the default bookmarked API settings to a few useful examples.
     */
	private void setDefaultBookmarkedApiSettings() {
		List<BookmarkedApiSettings> bookmarkedApiSettings = new ArrayList<>(Arrays.asList(
			new BookmarkedApiSettings("gpt-4-turbo", "https://api.openai.com/v1", "<YOUR KEY HERE>", 0.0),
			new BookmarkedApiSettings("anthropic/claude-3.5-sonnet", "https://openrouter.ai/api/v1","<YOUR KEY HERE>", 0.0),
			new BookmarkedApiSettings("llama.cpp", "http://localhost:8080/v1", "none", 0.0)));
        try {
        	Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
        } catch (IOException e) {
            Logger.warning("Failed to set default bookmarked API settings: " + e.getMessage());
        }	
	}
    
    /**
     * Sets the default value for the chat conversation in the preferences.
     * This method initializes the chat conversation setting to an empty string,
     * which might be used to represent no initial conversation history.
     *
     * @see Preferences#setDefault(String, String)
     */
    private void setDefaultChatConversation() {
        Preferences.getDefault().setDefault(PreferenceConstants.CHAT_CONVERSATION, "");
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