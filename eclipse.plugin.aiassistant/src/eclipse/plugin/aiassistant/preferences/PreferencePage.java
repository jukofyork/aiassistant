package eclipse.plugin.aiassistant.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.utility.DoubleFieldEditor;
import eclipse.plugin.aiassistant.utility.URLFieldEditor;

/**
 * PreferencePage manages the settings for the AI Assistant plugin.
 * It allows users to configure various aspects of the plugin such as API settings,
 * UI customization, and performance parameters.
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String API_BASE_URL_LABEL = "OpenAI API Base Address";
    private static final String API_MODEL_NAME_LABEL = "OpenAI API Model Name";
    private static final String API_KEY_LABEL = "OpenAI API Key";
    private static final String CONNECTION_TIMEOUT_LABEL = "Connection Timeout (ms)";
    private static final String TEMPERATURE_LABEL = "Temperature";
    private static final String CHAT_FONT_SIZE_LABEL = "Chat Message Font Size";
    private static final String NOTIFICATION_FONT_SIZE_LABEL = "Notification Font Size";
    private static final String DISABLE_TOOLTIPS_LABEL = "Disable Tooltips (Restart Required)";
    
    private static final String TOOLTIPS_TEXT = "The OpenAI API Base Address";
    
    /**
     * Constructs a new PreferencePage, setting the layout and initializing defaults.
     */
    public PreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getDefault());
        super.performDefaults();
    }

    /**
     * Initializes the preference page with the given workbench.
     *
     * @param workbench The workbench to initialize with.
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    /**
     * Creates the field editors for the preference page.
     */
    @Override
    protected void createFieldEditors() {
        addAPIBaseUrlFieldEditor();
        addAPIModelNameFieldEditor();
        addAPIKeyFieldEditor();
        addConnectionTimeoutFieldEditor();
        addTemperatureFieldEditor();
        addChatFontSizeFieldEditor();
        addNotificationFontSizeFieldEditor();
        addDisableTooltipsFieldEditor();
    }

    private void addAPIBaseUrlFieldEditor() {
        URLFieldEditor urlFieldEditor = new URLFieldEditor(PreferenceConstants.API_BASE_URL, API_BASE_URL_LABEL,
                getFieldEditorParent());
        urlFieldEditor.setToolTipText(TOOLTIPS_TEXT);
        addField(urlFieldEditor);
    }
    
    private void addAPIModelNameFieldEditor() {
        StringFieldEditor modelNameFieldEditor = new StringFieldEditor(PreferenceConstants.API_MODEL_NAME, API_MODEL_NAME_LABEL,
                getFieldEditorParent());
        addField(modelNameFieldEditor);
    }
    
    private void addAPIKeyFieldEditor() {
        StringFieldEditor apiKeyFieldEditor = new StringFieldEditor(PreferenceConstants.API_KEY, API_KEY_LABEL,
                getFieldEditorParent());
        addField(apiKeyFieldEditor);
    }
    
    private void addConnectionTimeoutFieldEditor() {
        IntegerFieldEditor connectionTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.CONNECTION_TIMEOUT,
                CONNECTION_TIMEOUT_LABEL, getFieldEditorParent());
        connectionTimeoutEditor.setValidRange(Constants.MIN_CONNECTION_TIMEOUT, Constants.MAX_CONNECTION_TIMEOUT);
        addField(connectionTimeoutEditor);
    }
    
    private void addTemperatureFieldEditor() {
        DoubleFieldEditor temperatureEditor = new DoubleFieldEditor(PreferenceConstants.TEMPERATURE, TEMPERATURE_LABEL,
                getFieldEditorParent());
        temperatureEditor.setValidRange(Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);
        addField(temperatureEditor);
    }
    
    private void addChatFontSizeFieldEditor() {
        IntegerFieldEditor chatFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.CHAT_FONT_SIZE, CHAT_FONT_SIZE_LABEL,
                getFieldEditorParent());
        chatFontSizeEditor.setValidRange(Constants.MIN_CHAT_FONT_SIZE, Constants.MAX_CHAT_FONT_SIZE);
        addField(chatFontSizeEditor);
    }
    
    private void addNotificationFontSizeFieldEditor() {
        IntegerFieldEditor notificationFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.NOTIFICATION_FONT_SIZE,
                NOTIFICATION_FONT_SIZE_LABEL, getFieldEditorParent());
        notificationFontSizeEditor.setValidRange(Constants.MIN_NOTIFICATION_FONT_SIZE,
                Constants.MAX_NOTIFICATION_FONT_SIZE);
        addField(notificationFontSizeEditor);
    }
    
    private void addDisableTooltipsFieldEditor() {
        BooleanFieldEditor disableTooltipsEditor = new BooleanFieldEditor(PreferenceConstants.DISABLE_TOOLTIPS,
                DISABLE_TOOLTIPS_LABEL, getFieldEditorParent());
        addField(disableTooltipsEditor);
    }
    
    @Override
    protected void performApply() {
        performOk();  // Save the preferences immediately on apply.
    }
    
    @Override
    public boolean performOk() {
        return super.performOk();
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
    }
}