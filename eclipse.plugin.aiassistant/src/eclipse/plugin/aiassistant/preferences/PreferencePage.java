package eclipse.plugin.aiassistant.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.network.OpenAiApiClient;
import eclipse.plugin.aiassistant.utility.DoubleFieldEditor;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.URLFieldEditor;

/**
 * PreferencePage manages the settings for the AI Assistant plugin.
 * It allows users to configure various aspects of the plugin such as API settings,
 * UI customization, and performance parameters.
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final int NUM_LIST_COLUMNS = 4;
	
    private static final String API_BASE_URL_LABEL = "OpenAI API Base Address";
    private static final String API_KEY_LABEL = "OpenAI API Key";
    private static final String CONNECTION_TIMEOUT_LABEL = "Connection Timeout (ms)";
    private static final String TEMPERATURE_LABEL = "Temperature";
    private static final String CHAT_FONT_SIZE_LABEL = "Chat Message Font Size";
    private static final String NOTIFICATION_FONT_SIZE_LABEL = "Notification Font Size";
    private static final String USE_STREAMING_LABEL = "Use Streaming";
    private static final String DISABLE_TOOLTIPS_LABEL = "Disable Tooltips (Restart Required)";
    
    private static final String TOOLTIPS_TEXT = "The OpenAI API Base Address";
    
	private OpenAiApiClient openAiApiClient;

	private Label label;
	private List[] modelLists = new List[NUM_LIST_COLUMNS];
    
    /**
     * Constructs a new PreferencePage, setting the layout and initializing defaults.
     */
    public PreferencePage() {
        super(GRID);
        openAiApiClient = new OpenAiApiClient();
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
        addAPIKeyFieldEditor();
        addConnectionTimeoutFieldEditor();
        addTemperatureFieldEditor();
        addChatFontSizeFieldEditor();
        addNotificationFontSizeFieldEditor();
        addUseStreamingFieldEditor();
        addDisableTooltipsFieldEditor();
        addModelListLabel();
        addModelList();
    }

    private void addAPIBaseUrlFieldEditor() {
        URLFieldEditor urlFieldEditor = new URLFieldEditor(PreferenceConstants.API_BASE_URL, API_BASE_URL_LABEL,
                getFieldEditorParent());
        urlFieldEditor.setToolTipText(TOOLTIPS_TEXT);
        addField(urlFieldEditor);
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
    
	private void addUseStreamingFieldEditor() {
		BooleanFieldEditor useStreamingEditor = new BooleanFieldEditor(PreferenceConstants.USE_STREAMING, USE_STREAMING_LABEL,
				getFieldEditorParent());
		addField(useStreamingEditor);
	}

    private void addDisableTooltipsFieldEditor() {
        BooleanFieldEditor disableTooltipsEditor = new BooleanFieldEditor(PreferenceConstants.DISABLE_TOOLTIPS,
                DISABLE_TOOLTIPS_LABEL, getFieldEditorParent());
        addField(disableTooltipsEditor);
    }

	private void addModelListLabel() {
		label = new Label(getFieldEditorParent(), SWT.NONE);
		GridData labelGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		labelGridData.horizontalSpan = 2;
		label.setLayoutData(labelGridData);
		label.setText("\nSelect Model:");
		label.setVisible(false);
	}
	
    private void addModelList() {
        Composite listComposite = new Composite(getFieldEditorParent(), SWT.NONE);
        GridLayout gridLayout = new GridLayout(NUM_LIST_COLUMNS, true);
        listComposite.setLayout(gridLayout);
        listComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        SelectionAdapter selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List sourceList = (List) e.widget;
                for (List list : modelLists) {
                    if (list != sourceList) {
                        list.deselectAll();
                    }
                }
            }
        };

        for (int i = 0; i < NUM_LIST_COLUMNS; i++) {
            modelLists[i] = createModelList(listComposite);
            modelLists[i].addSelectionListener(selectionListener);
        }
        populateModelList();
    }

	
    @Override
    protected void performApply() {
		Eclipse.runOnUIThreadAsync(() -> { populateModelList();	});
        performOk();  // Save the preferences immediately on apply.
    }
    
    @Override
    public boolean performOk() {
        String selectedModelName = null;
        for (List list : modelLists) {
            int selectedIndex = list.getSelectionIndex();
            if (selectedIndex != -1) {
                selectedModelName = list.getItem(selectedIndex);
                break;
            }
        }

        if (selectedModelName != null && !selectedModelName.equals(openAiApiClient.getLastSelectedModelId())) {
            getPreferenceStore().setValue(PreferenceConstants.LAST_SELECTED_MODEL_ID, selectedModelName);
        }

        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
		getPreferenceStore().setValue(PreferenceConstants.LAST_SELECTED_MODEL_ID, ""); // Just set to none.
		Eclipse.runOnUIThreadAsync(() -> { populateModelList();	});
    }
    

	/**
	 * Creates a list of available models.
	 *
	 * @param parent The parent composite for the list.
	 * @return The created list.
	 */
    public List createModelList(Composite parent) {
        List list = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        list.setLayoutData(gridData);
        list.setToolTipText("Select Model");
        list.setVisible(false);
        return list;
    }

	/**
	 * Populates the given list with the available models.
	 *
	 * @param list The list to populate.
	 */
    public void populateModelList() {
        if (modelLists[0] != null && !modelLists[0].isDisposed()) {
        	label.setVisible(false);
            for (List list : modelLists) {
                list.removeAll();
                list.setVisible(false);
            }
    
            String serverStatus = openAiApiClient.getCurrentServerStatus();
            if (!serverStatus.startsWith("No OpenAI compatible server found")) {
                java.util.List<String> modelNames = openAiApiClient.fetchModelIds();
                int totalModels = modelNames.size();
                
                if (totalModels > 0) {
	                int modelsPerList = (int) Math.ceil((double) totalModels / NUM_LIST_COLUMNS);
	    
	                // First loop: Add all model names to the lists
	                for (int i = 0; i < totalModels; i++) {
	                    int listIndex = i / modelsPerList;
	                    if (listIndex < NUM_LIST_COLUMNS) { // Ensure no index out of bounds
	                        modelLists[listIndex].add(modelNames.get(i));
	                    }
	                }
	    
	                // Second loop: Set selection based on the last selected model name
	                String lastSelectedModelName = openAiApiClient.getLastSelectedModelId();
	                if (!lastSelectedModelName.isEmpty()) {
	                    for (int i = 0; i < totalModels; i++) {
	                        int listIndex = i / modelsPerList;
	                        if (listIndex < NUM_LIST_COLUMNS && modelNames.get(i).equals(lastSelectedModelName)) {
	                            modelLists[listIndex].setSelection(i % modelsPerList);
	                            break; // Stop after the first match
	                        }
	                    }
	                }
	                label.setVisible(true);
	                for (List list : modelLists) {
	                    list.setVisible(true);
	                }
                }
            }

        }
    }

}