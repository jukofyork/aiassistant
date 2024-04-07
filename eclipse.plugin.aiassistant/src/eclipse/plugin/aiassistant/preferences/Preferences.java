package eclipse.plugin.aiassistant.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

import eclipse.plugin.aiassistant.Activator;
import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.utility.Eclipse;

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
	 * Returns the temperature value.
	 * 
	 * @return The temperature value.
	 */
	public static Double getTemperature() {
		return preferenceStore.getDouble(PreferenceConstants.TEMPERATURE);
	}

	/**
	 * Returns the repeat penalty value.
	 * 
	 * @return The repeat penalty value.
	 */
	public static Double getRepeatPenaltyValue() {
		return preferenceStore.getDouble(PreferenceConstants.REPEAT_PENALTY_VALUE);
	}

	/**
	 * Returns the repeat penalty window.
	 * 
	 * @return The repeat penalty window.
	 */
	public static Integer getRepeatPenaltyWindow() {
		return preferenceStore.getInt(PreferenceConstants.REPEAT_PENALTY_WINDOW);
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
	 * whether to use streaming.
	 * 
	 * @return A boolean value indicating whether to use streaming.
	 */
	public static boolean useStreaming() {
		return preferenceStore.getBoolean(PreferenceConstants.USE_STREAMING);
	}

	/**
	 * This method retrieves a boolean value from the preference store indicating
	 * whether to use the keep-alive service.
	 * 
	 * @return A boolean value indicating whether to use the keep-alive service.
	 */
	public static boolean useKeepaliveService() {
		return preferenceStore.getBoolean(PreferenceConstants.USE_KEEPALIVE_SERVICE);
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
	 * Returns the last selected model name.
	 * 
	 * @return The last selected model name.
	 */
	public static String getLastSelectedModelName() {
		return preferenceStore.getString(PreferenceConstants.LAST_SELECTED_MODEL_NAME);
	}

	/**
	 * Returns the base URL of the API.
	 * 
	 * @return The base URL of the API.
	 */
	public static URL getApiBaseUrl() {
		return getApiEndpoint("");
	}

	/**
	 * Returns the completion API endpoint URL.
	 * 
	 * @return The completion API endpoint URL.
	 */
	public static URL getCompletionApiEndpoint() {
		return getApiEndpoint(Constants.COMPLETION_API_URL);
	}

	/**
	 * Returns the chat completion API endpoint URL.
	 * 
	 * @return The chat completion API endpoint URL.
	 */
	public static URL getChatCompletionApiEndpoint() {
		return getApiEndpoint(Constants.CHAT_COMPLETION_API_URL);
	}

	/**
	 * Returns the model list API endpoint URL.
	 * 
	 * @return The model list API endpoint URL.
	 */
	public static URL getModelListApiEndpoint() {
		return getApiEndpoint(Constants.MODEL_LIST_API_URL);
	}

	/**
	 * Returns the API endpoint URL for a given path.
	 * 
	 * @param path The path to append to the base API URL.
	 * @return The API endpoint URL.
	 */
	private static URL getApiEndpoint(String path) {
		try {
			URL baseUrl = new URL(preferenceStore.getString(PreferenceConstants.API_BASE_URL));
			return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), path);
		} catch (MalformedURLException e) {
			Logger.error("Invalid API URL", e);
			throw new RuntimeException("Invalid API URL", e);
		}
	}

}