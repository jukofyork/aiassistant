package eclipse.plugin.aiassistant.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.network.OllamaModelManager;
import eclipse.plugin.aiassistant.utility.DoubleFieldEditor;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.URLFieldEditor;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String API_BASE_URL_LABEL = "Ollama API Base Address";
	private static final String CONNECTION_TIMEOUT_LABEL = "Connection Timeout (ms)";
	private static final String TEMPERATURE_LABEL = "Temperature";
	private static final String REPEAT_PENALTY_VALUE_LABEL = "Repeat Penalty Scaler";
	private static final String REPEAT_PENALTY_WINDOW_LABEL = "Repeat Penalty Window";
	private static final String CHAT_FONT_SIZE_LABEL = "Chat Message Font Size";
	private static final String NOTIFICATION_FONT_SIZE_LABEL = "Notification Font Size";
	private static final String USE_STREAMING_LABEL = "Use Streaming";
	private static final String USE_KEEPALIVE_SERVICE_LABEL = "Use Keepalive";
	private static final String DISABLE_TOOLTIPS_LABEL = "Disable Tooltips (Restart Required)";
	
	private static final String TOOLTIPS_TEXT = "Also check 'OLLAMA_HOST' and 'OLLAMA_ORIGINS' environment variables";

	private OllamaModelManager ollamaModelManager;

	private Label label;
	private List modelList;

	/**
     * Constructor for the PreferencePage class.
     */
	public PreferencePage() {
		super(GRID);
		ollamaModelManager = new OllamaModelManager();
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
	public void createFieldEditors() {
		addAPIBaseUrlFieldEditor();
		addConnectionTimeoutFieldEditor();
		addTemperatureFieldEditor();
		addRepeatPenaltyValueFieldEditor();
		addRepeatPenaltyWindowFieldEditor();
		addChatFontSizeFieldEditor();
		addNotificationFontSizeFieldEditor();
		addUseStreamingFieldEditor();
		addUseKeepaliveServiceFieldEditor();
		addDisableTooltipsFieldEditor();
		addModelList();
	}

	/**
	 * Adds a URL field editor for the API base URL preference.
	 */
	private void addAPIBaseUrlFieldEditor() {
		URLFieldEditor urlFieldEditor = new URLFieldEditor(PreferenceConstants.API_BASE_URL, API_BASE_URL_LABEL,
				getFieldEditorParent());
		urlFieldEditor.setToolTipText(TOOLTIPS_TEXT);
		addField(urlFieldEditor);
	}
	
	/**
	 * Adds an integer field editor for the connection timeout preference.
	 */
	private void addConnectionTimeoutFieldEditor() {
		IntegerFieldEditor connectionTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.CONNECTION_TIMEOUT,
				CONNECTION_TIMEOUT_LABEL, getFieldEditorParent());
		connectionTimeoutEditor.setValidRange(Constants.MIN_CONNECTION_TIMEOUT, Constants.MAX_CONNECTION_TIMEOUT);
		addField(connectionTimeoutEditor);
	}
	
	/**
	 * Adds a double field editor for the temperature preference.
	 */
	private void addTemperatureFieldEditor() {
		DoubleFieldEditor temperatureEditor = new DoubleFieldEditor(PreferenceConstants.TEMPERATURE, TEMPERATURE_LABEL,
				getFieldEditorParent());
		temperatureEditor.setValidRange(Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);
		addField(temperatureEditor);
	}
	
	/**
	 * Adds a double field editor for the repeat penalty value preference.
	 */
	private void addRepeatPenaltyValueFieldEditor() {
		DoubleFieldEditor repeatPenaltyEditor = new DoubleFieldEditor(PreferenceConstants.REPEAT_PENALTY_VALUE,
				REPEAT_PENALTY_VALUE_LABEL, getFieldEditorParent());
		repeatPenaltyEditor.setValidRange(Constants.MIN_REPEAT_PENALTY_VALUE, Constants.MAX_REPEAT_PENALTY_VALUE);
		addField(repeatPenaltyEditor);
	}
	
	/**
	 * Adds an integer field editor for the repeat penalty window preference.
	 */
	private void addRepeatPenaltyWindowFieldEditor() {
		IntegerFieldEditor repeatLastNEditor = new IntegerFieldEditor(PreferenceConstants.REPEAT_PENALTY_WINDOW,
				REPEAT_PENALTY_WINDOW_LABEL, getFieldEditorParent());
		repeatLastNEditor.setValidRange(Constants.MIN_REPEAT_PENALTY_WINDOW, Constants.MAX_REPEAT_PENALTY_WINDOW);
		addField(repeatLastNEditor);
	}
	
	/**
	 * Adds an integer field editor for the chat font size preference.
	 */
	private void addChatFontSizeFieldEditor() {
		IntegerFieldEditor chatFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.CHAT_FONT_SIZE, CHAT_FONT_SIZE_LABEL,
				getFieldEditorParent());
		chatFontSizeEditor.setValidRange(Constants.MIN_CHAT_FONT_SIZE, Constants.MAX_CHAT_FONT_SIZE);
		addField(chatFontSizeEditor);
	}
	
	/**
	 * Adds an integer field editor for the notification font size preference.
	 */
	private void addNotificationFontSizeFieldEditor() {
		IntegerFieldEditor notificationFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.NOTIFICATION_FONT_SIZE,
				NOTIFICATION_FONT_SIZE_LABEL, getFieldEditorParent());
		notificationFontSizeEditor.setValidRange(Constants.MIN_NOTIFICATION_FONT_SIZE,
				Constants.MAX_NOTIFICATION_FONT_SIZE);
		addField(notificationFontSizeEditor);
	}
	
	/**
	 * Adds a boolean field editor for the use streaming preference.
	 */
	private void addUseStreamingFieldEditor() {
		BooleanFieldEditor useStreamingEditor = new BooleanFieldEditor(PreferenceConstants.USE_STREAMING, USE_STREAMING_LABEL,
				getFieldEditorParent());
		addField(useStreamingEditor);
	}
	
	/**
	 * Adds a boolean field editor for the use keepalive service preference.
	 */
	private void addUseKeepaliveServiceFieldEditor() {
		BooleanFieldEditor useKeepaliveEditor = new BooleanFieldEditor(PreferenceConstants.USE_KEEPALIVE_SERVICE,
				USE_KEEPALIVE_SERVICE_LABEL, getFieldEditorParent());
		addField(useKeepaliveEditor);
	}
	
	/**
	 * Adds a boolean field editor for the disable tooltips preference.
	 */
	private void addDisableTooltipsFieldEditor() {
		BooleanFieldEditor disableTooltipsEditor = new BooleanFieldEditor(PreferenceConstants.DISABLE_TOOLTIPS,
				DISABLE_TOOLTIPS_LABEL, getFieldEditorParent());
		addField(disableTooltipsEditor);
	}
	
	/**
	 * Adds a label and a list for selecting models.
	 */
	private void addModelList() {
		label = new Label(getFieldEditorParent(), SWT.NONE);
		GridData labelGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		labelGridData.horizontalSpan = 2;
		label.setLayoutData(labelGridData);
		label.setText("\nSelect Model:");
		modelList = createModelList(getFieldEditorParent());
		populateModelList();
	}

	/**
	 * Updates the widgets to reflect changes when the "Apply" button is pressed.
	 *
	 * @return Whether the default action was performed successfully.
	 */
	protected void performApply() {
		if (modelList != null && !modelList.isDisposed()) {
			Eclipse.runOnUIThreadAsync(() -> {
				populateModelList();
				modelList.requestLayout();
			});
		}
		performOk();	// To save the preferences.
	}
	
	/**
	 * Saves the preferences when "Apply and Close" if pressed.
	 *
	 * @return Whether the default action was performed successfully.
	 */
	@Override
	public boolean performOk() {
		if (modelList != null && !modelList.isDisposed()) {
			int selectedIndex = modelList.getSelectionIndex();
			if (selectedIndex != -1) {
				String modelName = modelList.getItem(selectedIndex);
				if (!modelName.equals(ollamaModelManager.getLastSelectedModelName())) {
					getPreferenceStore().setValue(PreferenceConstants.LAST_SELECTED_MODEL_NAME, modelName);
					ollamaModelManager.attemptLoadLastSelectedModelIntoMemory();
				}
			}
		}
		return super.performOk();
	}

	/**
	 * Resets the preference page to its default values.
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();
		getPreferenceStore().setValue(PreferenceConstants.LAST_SELECTED_MODEL_NAME, ""); // Just set to none.
		Eclipse.runOnUIThreadSync(() -> {	// Run synchronously to avoid disposed page problem.
			populateModelList();
			modelList.deselectAll();
			modelList.requestLayout();
		});
	}

	/**
	 * Creates a list of available models.
	 *
	 * @param parent The parent composite for the list.
	 * @return The created list.
	 */
	public List createModelList(Composite parent) {
		List list = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData listGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		listGridData.horizontalSpan = 2;
		list.setLayoutData(listGridData);
		list.setToolTipText("Select Model");
		return list;
	}

	/**
	 * Populates the given list with the available models.
	 *
	 * @param list The list to populate.
	 */
	public void populateModelList() {
		if (modelList != null && !modelList.isDisposed()) {	// Check disposed page problem.
			modelList.removeAll();
			String serverStatus = ollamaModelManager.getCurrentServerStatus();
			if (serverStatus.startsWith("No Ollama server found")) {
				modelList.add(serverStatus); // Will help user see there is a problem.
			} else {
				String[] modelNames = ollamaModelManager.fetchAvailableModelNames();
				modelList.setItems(modelNames);
				String modelName = ollamaModelManager.getLastSelectedModelName();
				if (!modelName.isEmpty() && modelList.indexOf(modelName) != -1) {
					modelList.setSelection(modelList.indexOf(modelName)); // Set to last clicked.
				}
			}
		}
	}

}