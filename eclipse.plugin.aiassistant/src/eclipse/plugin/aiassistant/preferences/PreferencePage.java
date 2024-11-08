package eclipse.plugin.aiassistant.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.network.OpenAiApiClient;
import eclipse.plugin.aiassistant.utility.DoubleFieldEditor;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.URLFieldEditor;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	    
    // General settings editors
    private IntegerFieldEditor connectionTimeoutEditor;
    private IntegerFieldEditor chatFontSizeEditor;
    private IntegerFieldEditor notificationFontSizeEditor;
    private BooleanFieldEditor streamingEditor;
    private BooleanFieldEditor disableTooltipsEditor;
    
    // Current API settings editors
    private StringFieldEditor modelNameEditor;
    private URLFieldEditor apiUrlEditor;
    private StringFieldEditor apiKeyEditor;
    private DoubleFieldEditor temperatureEditor;
    
    // Bookmarked API settings table
    private TableViewer tableViewer;
    
    private List<BookmarkedApiSettings> bookmarkedApiSettings;
    
    private OpenAiApiClient openAiApiClient;

    public PreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getDefault());
        try {
			bookmarkedApiSettings = Preferences.loadBookmarkedApiSettings();
		} catch (Exception e) {
			Logger.warning("Failed to load bookmarked API settings: " + e.getMessage());
		}
        openAiApiClient = new OpenAiApiClient();
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 3;
        layout.marginHeight = 3;
        parent.setLayout(layout);
        
        // Add a section header label
        Label sectionLabelGeneral = new Label(parent, SWT.NONE);
        sectionLabelGeneral.setText("GENERAL SETTINGS:");
        GridData gridDataGeneral = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridDataGeneral.horizontalSpan = 2;
        sectionLabelGeneral.setLayoutData(gridDataGeneral);
        
        // Create global settings group with full-width GridData
        createGlobalSettingsGroup(parent);
        
        // Add a section header label
        Label sectionLabelCurrent = new Label(parent, SWT.NONE);
        sectionLabelCurrent.setText("\nCURRENT API SETTINGS:");
        GridData gridDataCurrent = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridDataCurrent.horizontalSpan = 2;
        sectionLabelCurrent.setLayoutData(gridDataCurrent);

        // Create API settings group with full-width GridData
        createCurrentApiSettingsGroup(parent);
        
        // Add a section header label
        Label sectionLabelBookmarked = new Label(parent, SWT.NONE);
        sectionLabelBookmarked.setText("\nBOOKMARKED API SETTINGS:");
        GridData gridDataBookmarked = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridDataBookmarked.horizontalSpan = 2;
        sectionLabelBookmarked.setLayoutData(gridDataBookmarked);

        // Create quick select table with full-width GridData
        createTable(parent);
        createActionButtons(parent);

    }
    
    private void createGlobalSettingsGroup(Composite parent) {
        connectionTimeoutEditor = new IntegerFieldEditor(
            PreferenceConstants.CONNECTION_TIMEOUT,
            "Connection Timeout (ms):",
            parent);
        connectionTimeoutEditor.setValidRange(Constants.MIN_CONNECTION_TIMEOUT, Constants.MAX_CONNECTION_TIMEOUT);
    
        chatFontSizeEditor = new IntegerFieldEditor(
            PreferenceConstants.CHAT_FONT_SIZE,
            "Chat Font Size:",
            parent);
        chatFontSizeEditor.setValidRange(Constants.MIN_CHAT_FONT_SIZE, Constants.MAX_CHAT_FONT_SIZE);
    
        notificationFontSizeEditor = new IntegerFieldEditor(
            PreferenceConstants.NOTIFICATION_FONT_SIZE,
            "Notification Font Size:",
            parent);
        notificationFontSizeEditor.setValidRange(Constants.MIN_NOTIFICATION_FONT_SIZE, Constants.MAX_NOTIFICATION_FONT_SIZE);
    
        streamingEditor = new BooleanFieldEditor(
                PreferenceConstants.USE_STREAMING,
                "Use Streaming",
                BooleanFieldEditor.SEPARATE_LABEL,
                parent);
        
        disableTooltipsEditor = new BooleanFieldEditor(
            PreferenceConstants.DISABLE_TOOLTIPS,
            "Disable Tooltips",
            BooleanFieldEditor.SEPARATE_LABEL,
            parent);
    
        addField(connectionTimeoutEditor);
        addField(chatFontSizeEditor);
        addField(notificationFontSizeEditor);
        addField(streamingEditor);
        addField(disableTooltipsEditor);
        
    }
    
    private void createCurrentApiSettingsGroup(Composite parent) {       
        modelNameEditor = new StringFieldEditor(
                PreferenceConstants.CURRENT_MODEL_NAME,
                "Model Name:",
                parent);
        
        apiUrlEditor = new URLFieldEditor(
            PreferenceConstants.CURRENT_API_URL,
            "API URL:",
            parent);
        
        apiKeyEditor = new StringFieldEditor(
            PreferenceConstants.CURRENT_API_KEY,
            "API Key:",
            parent);

        temperatureEditor = new DoubleFieldEditor(
            PreferenceConstants.CURRENT_TEMPERATURE,
            "Temperature:",
            parent);
        temperatureEditor.setValidRange(Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);
            
        addField(modelNameEditor);
        addField(apiUrlEditor);
        addField(apiKeyEditor);
        addField(temperatureEditor);

    }
    
    private void createTable(Composite parent) {   
        // Create a scrolled composite
        org.eclipse.swt.custom.ScrolledComposite scrolledComposite = 
            new org.eclipse.swt.custom.ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData scrolledData = new GridData(SWT.FILL, SWT.FILL, true, false);
        scrolledData.heightHint = 300;
        scrolledData.horizontalSpan = 2;
        scrolledComposite.setLayoutData(scrolledData);
        
        // Create the table composite inside the scrolled composite
        Composite tableComposite = new Composite(scrolledComposite, SWT.NONE);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
    
        // Create the table viewer inside the composite
        tableViewer = new TableViewer(tableComposite, SWT.FULL_SELECTION);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
    
        // Create all columns with weight-based widths
        createTableColumn("Model Name", 25, tableLayout, e -> ((BookmarkedApiSettings)e).getModelName());
        createTableColumn("API URL", 25, tableLayout, e -> ((BookmarkedApiSettings)e).getApiUrl());
        createTableColumn("API Key", 40, tableLayout, e -> ((BookmarkedApiSettings)e).getApiKey());
        createTableColumn("Temperature", 10, tableLayout, e -> String.valueOf(((BookmarkedApiSettings)e).getTemperature()));
    
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = table.getSelection();
                if (items.length > 0) {
                    BookmarkedApiSettings selected = (BookmarkedApiSettings)items[0].getData();
                    populatePreferenceEditors(selected);
                }
            }
        });
    
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setInput(bookmarkedApiSettings);
    
        // Set the scrolled content and configure it
        scrolledComposite.setContent(tableComposite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(tableComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
        
    private void createActionButtons(Composite parent) {
       Composite buttonComposite = new Composite(parent, SWT.NONE);
       GridLayout layout = new GridLayout(10, true);
       buttonComposite.setLayout(layout);
       GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
       gridData.horizontalSpan = 2;
       buttonComposite.setLayoutData(gridData);
   
       // Bookmark button
       Eclipse.createButton(buttonComposite, 
           "Bookmark", 
           "Bookmark current API settings",
           "Bookmark.png",
           new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
                   performApply();
                   
                   // Check if settings already exist before adding
                   BookmarkedApiSettings currentSettings = new BookmarkedApiSettings(
                       getPreferenceStore().getString(PreferenceConstants.CURRENT_MODEL_NAME),
                       getPreferenceStore().getString(PreferenceConstants.CURRENT_API_URL),
                       getPreferenceStore().getString(PreferenceConstants.CURRENT_API_KEY),
                       getPreferenceStore().getDouble(PreferenceConstants.CURRENT_TEMPERATURE)
                   );
                   boolean exists = bookmarkedApiSettings.contains(currentSettings);
                   
                   if (!exists) {
                       bookmarkedApiSettings.add(new BookmarkedApiSettings(
                           getPreferenceStore().getString(PreferenceConstants.CURRENT_MODEL_NAME),
                           getPreferenceStore().getString(PreferenceConstants.CURRENT_API_URL),
                           getPreferenceStore().getString(PreferenceConstants.CURRENT_API_KEY),
                           getPreferenceStore().getDouble(PreferenceConstants.CURRENT_TEMPERATURE)
                       ));
					   try {
						   Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
					   } catch (IOException exception) {
						   Logger.warning("Failed to set bookmarked API settings: " + exception.getMessage());
					   }
                       tableViewer.refresh();
                   }
               }
           }
       );
       
       // Unbookmark button
       Eclipse.createButton(buttonComposite, 
           "Unbookmark", 
           "Unbookmark matching API settings",
           "Unbookmark.png",
           new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
					performApply();
					BookmarkedApiSettings currentSettings = new BookmarkedApiSettings(
						getPreferenceStore().getString(PreferenceConstants.CURRENT_MODEL_NAME),
						getPreferenceStore().getString(PreferenceConstants.CURRENT_API_URL),
						getPreferenceStore().getString(PreferenceConstants.CURRENT_API_KEY),
						getPreferenceStore().getDouble(PreferenceConstants.CURRENT_TEMPERATURE));
					bookmarkedApiSettings.remove(currentSettings);
					try {
						Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
					} catch (IOException exception) {
						Logger.warning("Failed to set bookmarked API settings: " + exception.getMessage());
					}
					tableViewer.refresh();
               }
           }
       );
   
       // Clear button
       Eclipse.createButton(buttonComposite, 
           "Clear",
           "Clear all bookmarked API settings",
           "Clear.png",
           new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
            	   bookmarkedApiSettings.clear();
            	   try {
            		   Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
            	   } catch (IOException exception) {
            		   Logger.warning("Failed to set bookmarked API settings: " + exception.getMessage());
            	   }
                   tableViewer.refresh();
               }
           }
       );
       
       // Sort button
       Eclipse.createButton(buttonComposite, 
           "Sort",
           "Sort bookmarked API settings",
           "Sort.png",
           new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
            	   bookmarkedApiSettings.sort(null);
            	   try {
            		   Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
            	   } catch (IOException exception) {
            		   Logger.warning("Failed to set bookmarked API settings: " + exception.getMessage());
            	   }
                   tableViewer.refresh();
               }
           }
       );
       
       // Populate button
       Eclipse.createButton(buttonComposite, 
           "Populate",
           "Populate API settings",
           "Populate.png",
           new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
                   String serverStatus = openAiApiClient.getCurrentServerStatus();
                   if (!serverStatus.startsWith("No OpenAI compatible server found")) {
                       java.util.List<String> modelNames = openAiApiClient.fetchModelIds();
                       String currentModelName = modelNameEditor.getStringValue().trim();
                       String currentApiUrl = apiUrlEditor.getStringValue().trim();
                       String currentApiKey = apiKeyEditor.getStringValue().trim();
                       double currentTemp = temperatureEditor.getDoubleValue();
                       
                       for (String modelName : modelNames) {
                           // If current model name is empty or modelName is a substring of current model name
                           if (currentModelName.isEmpty() || 
                               modelName.toLowerCase().contains(currentModelName.toLowerCase())) {
                               
                               BookmarkedApiSettings newSetting = new BookmarkedApiSettings(
                                   modelName,
                                   currentApiUrl,
                                   currentApiKey,
                                   currentTemp
                               );
                               
                               // Add if not already present
                               if (!bookmarkedApiSettings.contains(newSetting)) {
                                   bookmarkedApiSettings.add(newSetting);
                               }
                           }
                       }
                       
                       try {
                           Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
                       } catch (IOException exception) {
                           Logger.warning("Failed to set bookmarked API settings: " + exception.getMessage());
                       }
                       tableViewer.refresh();
                   }
               }
           }
       );
          
   }
    
    private void createTableColumn(String title, int weight, TableColumnLayout tableLayout, Function<Object, String> labelProvider) {
        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setResizable(true);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return labelProvider.apply(element);
            }
        });
        
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
    }

    private void populatePreferenceEditors(BookmarkedApiSettings settings) {
        getPreferenceStore().setValue(PreferenceConstants.CURRENT_MODEL_NAME, settings.getModelName());
        getPreferenceStore().setValue(PreferenceConstants.CURRENT_API_URL, settings.getApiUrl());
        getPreferenceStore().setValue(PreferenceConstants.CURRENT_API_KEY, settings.getApiKey());
        getPreferenceStore().setValue(PreferenceConstants.CURRENT_TEMPERATURE, settings.getTemperature());
        
        // Reload all editors from the preference store
        modelNameEditor.load();
        apiUrlEditor.load();
        apiKeyEditor.load();
        temperatureEditor.load();
    }

    @Override
    public boolean performOk() {
        return super.performOk();
    }
    
    @Override
    protected void performApply() {
        performOk();  // Save the preferences immediately on apply.
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
		bookmarkedApiSettings = new ArrayList<>(Arrays.asList(
				new BookmarkedApiSettings("gpt-4-turbo", "https://api.openai.com/v1", "<YOUR KEY HERE>", 0.0),
				new BookmarkedApiSettings("anthropic/claude-3.5-sonnet", "https://openrouter.ai/api/v1", "<YOUR KEY HERE>", 0.0),
				new BookmarkedApiSettings("llama.cpp", "http://localhost:8080/v1", "none", 0.0)));
		try {
			Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
		} catch (IOException e) {
			Logger.warning("Failed to set default bookmarked API settings: " + e.getMessage());
		}
		tableViewer.setInput(bookmarkedApiSettings);
    }
}