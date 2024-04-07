package eclipse.plugin.aiassistant.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.prompt.PromptLoader;
import eclipse.plugin.aiassistant.prompt.Prompts;

/**
 * This class is responsible for initializing default preference values when the application starts.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /**
     * Initializes the default preference values.
     */
    public void initializeDefaultPreferences() {
        setDefaultAPIBaseURL();
        setDefaultConnectionTimeout();
        setDefaultTemperature();
        setDefaultRepeatPenaltyValue();
        setDefaultRepeatPenaltyWindow();
        setDefaultChatFontSize();
        setDefaultNotificationFontSize();
        setDefaultUseStreaming();
        setDefaultUseKeepaliveService();
        setDefaultDisableTooltips();
        setDefaultLastSelectedModelName();
        loadAndSetDefaultPrompts();
    }

    /**
     * Sets the default API base URL.
     */
    private void setDefaultAPIBaseURL() {
        Preferences.getDefault().setDefault(PreferenceConstants.API_BASE_URL, Constants.DEFAULT_API_BASE_URL);
    }

    /**
     * Sets the default connection timeout value.
     */
    private void setDefaultConnectionTimeout() {
        Preferences.getDefault().setDefault(PreferenceConstants.CONNECTION_TIMEOUT, Constants.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * Sets the default temperature value for the AI model.
     */
    private void setDefaultTemperature() {
        Preferences.getDefault().setDefault(PreferenceConstants.TEMPERATURE, Constants.DEFAULT_TEMPERATURE);
    }

    /**
     * Sets the default repeat penalty value for the AI model.
     */
    private void setDefaultRepeatPenaltyValue() {
        Preferences.getDefault().setDefault(PreferenceConstants.REPEAT_PENALTY_VALUE, Constants.DEFAULT_REPEAT_PENALTY_VALUE);
    }

    /**
     * Sets the default repeat penalty window size for the AI model.
     */
    private void setDefaultRepeatPenaltyWindow() {
        Preferences.getDefault().setDefault(PreferenceConstants.REPEAT_PENALTY_WINDOW, Constants.DEFAULT_REPEAT_PENALTY_WINDOW);
    }

    /**
     * Sets the default chat font size.
     */
    private void setDefaultChatFontSize() {
        Preferences.getDefault().setDefault(PreferenceConstants.CHAT_FONT_SIZE, Constants.DEFAULT_CHAT_FONT_SIZE);
    }

    /**
     * Sets the default notification font size.
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
     * Sets the default value for using keepalive service.
     */
    private void setDefaultUseKeepaliveService() {
        Preferences.getDefault().setDefault(PreferenceConstants.USE_KEEPALIVE_SERVICE, Constants.DEFAULT_USE_KEEPALIVE_SERVICE);
    }

    /**
     * Sets the default value for disabling tooltips.
     */
    private void setDefaultDisableTooltips() {
        Preferences.getDefault().setDefault(PreferenceConstants.DISABLE_TOOLTIPS, Constants.DEFAULT_DISABLE_TOOLTIPS);
    }

    /**
     * Sets the default last selected model name to "" (ie: none).
     */
    private void setDefaultLastSelectedModelName() {
        Preferences.getDefault().setDefault(PreferenceConstants.LAST_SELECTED_MODEL_NAME, "");
    }

    /**
     * Loads and sets the default prompts for each prompt type.
     */
    private void loadAndSetDefaultPrompts() {
        for (Prompts prompt : Prompts.values()) {
            Preferences.getDefault().setDefault(prompt.preferenceName(), PromptLoader.getRawPrompt(prompt.getFileName()));
        }
    }
    
}