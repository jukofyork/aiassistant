package eclipse.plugin.aiassistant.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import eclipse.plugin.aiassistant.Constants;
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
        setDefaultAPIBaseURL();
        setDefaultAPIModelName();
        setDefaultAPIKey();
        setDefaultConnectionTimeout();
        setDefaultTemperature();
        setDefaultChatFontSize();
        setDefaultNotificationFontSize();
        setDefaultDisableTooltips();
        loadAndSetDefaultPrompts();
    }

    /**
     * Sets the default API base URL in the preference store.
     */
    private void setDefaultAPIBaseURL() {
        Preferences.getDefault().setDefault(PreferenceConstants.API_BASE_URL, Constants.DEFAULT_API_BASE_URL);
    }

    /**
     * Sets the default model name for the AI API in the preference store.
     */
    private void setDefaultAPIModelName() {
        Preferences.getDefault().setDefault(PreferenceConstants.API_MODEL_NAME, Constants.DEFAULT_API_MODEL_NAME);
    }
    
    /**
     * Sets the default API key for accessing the AI service.
     */
    private void setDefaultAPIKey() {
        Preferences.getDefault().setDefault(PreferenceConstants.API_KEY, Constants.DEFAULT_API_KEY);
    }
    
    /**
     * Sets the default connection timeout for API requests.
     */
    private void setDefaultConnectionTimeout() {
        Preferences.getDefault().setDefault(PreferenceConstants.CONNECTION_TIMEOUT, Constants.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * Sets the default temperature setting for the AI model, affecting response variability.
     */
    private void setDefaultTemperature() {
        Preferences.getDefault().setDefault(PreferenceConstants.TEMPERATURE, Constants.DEFAULT_TEMPERATURE);
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
     * Loads default prompts from files and sets them in the preference store.
     * Each prompt type defined in {@link Prompts} is loaded and stored.
     */
    private void loadAndSetDefaultPrompts() {
        for (Prompts prompt : Prompts.values()) {
            Preferences.getDefault().setDefault(prompt.preferenceName(), PromptLoader.getRawPrompt(prompt.getFileName()));
        }
    }
    
}