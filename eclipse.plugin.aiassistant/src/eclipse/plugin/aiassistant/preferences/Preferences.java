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
	 * Returns the base URL of the API.
	 * 
	 * @return The base URL of the API.
	 */
	public static URL getApiBaseUrl() {
		return getApiEndpoint("");
	}
	
	public static String getApiKey() {
		return preferenceStore.getString(PreferenceConstants.API_KEY);
	}
	
	public static String getApiModelName() {
		return preferenceStore.getString(PreferenceConstants.API_MODEL_NAME);
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
	 * Returns the API endpoint URL for a given path.
	 * 
	 * @param path The path to append to the base API URL.
	 * @return The API endpoint URL.
	 */
	private static URL getApiEndpoint(String path) {
		try {
			URL baseUrl = new URL(preferenceStore.getString(PreferenceConstants.API_BASE_URL));
			return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), baseUrl.getFile() + path);
		} catch (MalformedURLException e) {
			Logger.error("Invalid API URL", e);
			throw new RuntimeException("Invalid API URL", e);
		}
	}

}