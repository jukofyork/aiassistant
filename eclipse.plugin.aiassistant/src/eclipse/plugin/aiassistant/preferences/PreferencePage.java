package eclipse.plugin.aiassistant.preferences;

import java.io.IOException;
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

/**
 * Preference page for the AI Assistant plugin that manages both general settings
 * and API configurations. This page allows users to:
 * <ul>
 *   <li>Configure general plugin behavior (timeouts, font sizes, etc.)</li>
 *   <li>Set up current API connection parameters</li>
 *   <li>Manage bookmarked API configurations</li>
 * </ul>
 * 
 * @see IWorkbenchPreferencePage
 * @see FieldEditorPreferencePage
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	 /** Field editors for general plugin settings */
	private IntegerFieldEditor connectionTimeoutEditor;
	private IntegerFieldEditor requestTimeoutEditor;
	private IntegerFieldEditor chatFontSizeEditor;
	private IntegerFieldEditor notificationFontSizeEditor;
	private BooleanFieldEditor streamingEditor;
	private BooleanFieldEditor disableTooltipsEditor;

	/** Field editors for current API configuration */
	private StringFieldEditor modelNameEditor;
	private URLFieldEditor apiUrlEditor;
	private StringFieldEditor apiKeyEditor;
	private DoubleFieldEditor temperatureEditor;

	 /** Table viewer for managing bookmarked API configurations */
	private TableViewer tableViewer;

	 /** List of saved API configurations */
	private List<BookmarkedApiSettings> bookmarkedApiSettings;

    /**
     * Creates a new preference page instance and initializes the preference store
     * and bookmarked API settings.
     */
	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Preferences.getDefault());
		try {
			bookmarkedApiSettings = Preferences.loadBookmarkedApiSettings();
		} catch (Exception e) {
			Logger.warning("Failed to load bookmarked API settings: " + e.getMessage());
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

    /**
     * Creates and initializes all field editors for this preference page.
     * Organizes the fields into three main sections:
     * - General Settings
     * - Current API Settings
     * - Bookmarked API Settings
     */
	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		parent.setLayout(layout);

		createSectionHeader(parent, "GENERAL SETTINGS:");
		createGlobalSettingsGroup(parent);

		createSectionHeader(parent, "\nCURRENT API SETTINGS:");
		createCurrentApiSettingsGroup(parent);

		createSectionHeader(parent, "\nBOOKMARKED API SETTINGS:");
		createTable(parent);
		createActionButtons(parent);
	}

    /**
     * Creates the general settings section with timeout and display preferences.
     * 
     * @param parent The parent composite where the fields will be created
     */
	private void createGlobalSettingsGroup(Composite parent) {
		connectionTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.CONNECTION_TIMEOUT,
				"Connection Timeout (ms):", parent);
		connectionTimeoutEditor.setValidRange(Constants.MIN_CONNECTION_TIMEOUT, Constants.MAX_CONNECTION_TIMEOUT);
		
		requestTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.REQUEST_TIMEOUT,
				"Request Timeout (ms):", parent);
		requestTimeoutEditor.setValidRange(Constants.MIN_REQUEST_TIMEOUT, Constants.MAX_REQUEST_TIMEOUT);

		chatFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.CHAT_FONT_SIZE, "Chat Font Size:", parent);
		chatFontSizeEditor.setValidRange(Constants.MIN_CHAT_FONT_SIZE, Constants.MAX_CHAT_FONT_SIZE);

		notificationFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.NOTIFICATION_FONT_SIZE,
				"Notification Font Size:", parent);
		notificationFontSizeEditor.setValidRange(Constants.MIN_NOTIFICATION_FONT_SIZE,
				Constants.MAX_NOTIFICATION_FONT_SIZE);

		streamingEditor = new BooleanFieldEditor(PreferenceConstants.USE_STREAMING, "Use Streaming",
				BooleanFieldEditor.SEPARATE_LABEL, parent);

		disableTooltipsEditor = new BooleanFieldEditor(PreferenceConstants.DISABLE_TOOLTIPS, "Disable Tooltips",
				BooleanFieldEditor.SEPARATE_LABEL, parent);

		addField(connectionTimeoutEditor);
		addField(requestTimeoutEditor);
		addField(chatFontSizeEditor);
		addField(notificationFontSizeEditor);
		addField(streamingEditor);
		addField(disableTooltipsEditor);
	}

    /**
     * Creates the current API settings section for configuring the active API connection.
     * 
     * @param parent The parent composite where the fields will be created
     */
	private void createCurrentApiSettingsGroup(Composite parent) {
		modelNameEditor = new StringFieldEditor(PreferenceConstants.CURRENT_MODEL_NAME, "Model Name:", parent);

		apiUrlEditor = new URLFieldEditor(PreferenceConstants.CURRENT_API_URL, "API URL:", parent);

		apiKeyEditor = new StringFieldEditor(PreferenceConstants.CURRENT_API_KEY, "API Key:", parent);

		temperatureEditor = new DoubleFieldEditor(PreferenceConstants.CURRENT_TEMPERATURE, "Temperature:", parent);
		temperatureEditor.setValidRange(Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);

		addField(modelNameEditor);
		addField(apiUrlEditor);
		addField(apiKeyEditor);
		addField(temperatureEditor);
	}

    /**
     * Creates a scrollable table for displaying and managing bookmarked API configurations.
     * 
     * @param parent The parent composite where the table will be created
     */
	private void createTable(Composite parent) {
		// Create a scrolled composite
		org.eclipse.swt.custom.ScrolledComposite scrolledComposite = new org.eclipse.swt.custom.ScrolledComposite(
				parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
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
		createTableColumn("Model Name", 25, tableLayout, e -> ((BookmarkedApiSettings) e).getModelName());
		createTableColumn("API URL", 25, tableLayout, e -> ((BookmarkedApiSettings) e).getApiUrl());
		createTableColumn("API Key", 40, tableLayout, e -> ((BookmarkedApiSettings) e).getApiKey());
		createTableColumn("Temperature", 10, tableLayout,
				e -> String.valueOf(((BookmarkedApiSettings) e).getTemperature()));

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items.length > 0) {
					BookmarkedApiSettings selectedSettings = (BookmarkedApiSettings) items[0].getData();
					modelNameEditor.setStringValue(selectedSettings.getModelName());
					apiUrlEditor.setStringValue(selectedSettings.getApiUrl());
					apiKeyEditor.setStringValue(selectedSettings.getApiKey());
					temperatureEditor.setStringValue(Double.toString(selectedSettings.getTemperature()));
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

    /**
     * Creates action buttons for managing bookmarked API settings.
     * Includes buttons for bookmark, unbookmark, clear, sort, and populate operations.
     * 
     * @param parent The parent composite where the buttons will be created
     */
	private void createActionButtons(Composite parent) {
	    Composite buttonComposite = new Composite(parent, SWT.NONE);
	    GridLayout layout = new GridLayout(10, true);
	    buttonComposite.setLayout(layout);
	    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
	    gridData.horizontalSpan = 2;
	    buttonComposite.setLayoutData(gridData);
	
	    // Bookmark button
	    createActionButton(buttonComposite, "Bookmark", "Bookmark current API settings", "Bookmark.png", () -> {
	        BookmarkedApiSettings currentSettings = getCurrentSettings();
	        if (!bookmarkedApiSettings.contains(currentSettings)) {
	            bookmarkedApiSettings.add(currentSettings);
	            tableViewer.refresh();
	        }
	    });
	
	    // Unbookmark button
	    createActionButton(buttonComposite, "Unbookmark", "Unbookmark matching API settings", "Unbookmark.png", () -> {
	        BookmarkedApiSettings currentSettings = getCurrentSettings();
	        bookmarkedApiSettings.remove(currentSettings);
	        tableViewer.refresh();
	    });
	
	    // Clear button
	    createActionButton(buttonComposite, "Clear", "Clear all bookmarked API settings", "Clear.png", () -> {
	        bookmarkedApiSettings.clear();
	        tableViewer.refresh();
	    });
	
	    // Sort button
	    createActionButton(buttonComposite, "Sort", "Sort bookmarked API settings", "Sort.png", () -> {
	        bookmarkedApiSettings.sort(null);
	        tableViewer.refresh();
	    });
	
		// Populate button
		createActionButton(buttonComposite, "Populate", "Populate API settings", "Populate.png", () -> {
			String currentModelName = modelNameEditor.getStringValue().trim();
			String currentApiUrl = apiUrlEditor.getStringValue().trim();
			String currentApiKey = apiKeyEditor.getStringValue().trim();
			double currentTemp = temperatureEditor.getDoubleValue();
			if (OpenAiApiClient.getApiStatus(currentApiUrl, currentApiKey).equals("OK")) {
				java.util.List<String> modelNames = OpenAiApiClient.fetchAvailableModelNames(currentApiUrl,
						currentApiKey);
				for (String modelName : modelNames) {
					// If current model name is a substring of model name (or empty)
					if (modelName.toLowerCase().contains(currentModelName.toLowerCase())) {
						BookmarkedApiSettings newSetting = new BookmarkedApiSettings(modelName, currentApiUrl,
								currentApiKey, currentTemp);
						if (!bookmarkedApiSettings.contains(newSetting)) {
							bookmarkedApiSettings.add(newSetting);
						}
					}
				}
				tableViewer.refresh();
			}
		});
	}
	
	   /**
     * Creates a single action button with specified parameters.
     * 
     * @param parent The parent composite
     * @param text Button label text
     * @param tooltip Button tooltip text
     * @param imageName Name of the button image resource
     * @param action Runnable to execute when button is clicked
     */
	private void createActionButton(Composite parent, String text, String tooltip, String imageName, Runnable action) {
	    Eclipse.createButton(parent, text, tooltip, imageName,
	        new SelectionAdapter() {
	            @Override
	            public void widgetSelected(SelectionEvent e) {
	                action.run();
	            }
	        });
	}

	   /**
     * Creates a table column with specified configuration.
     * 
     * @param title Column header text
     * @param weight Column width weight
     * @param tableLayout Layout manager for the table
     * @param labelProvider Function to extract display text from table elements
     */
	private void createTableColumn(String title, int weight, TableColumnLayout tableLayout,
			Function<Object, String> labelProvider) {
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
	
    /**
     * Creates a section header label with specified text.
     * 
     * @param parent The parent composite
     * @param text Header text
     * @return Created Label widget
     */
	private Label createSectionHeader(Composite parent, String text) {
	    Label sectionLabel = new Label(parent, SWT.NONE);
	    sectionLabel.setText(text);
	    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
	    gridData.horizontalSpan = 2;
	    sectionLabel.setLayoutData(gridData);
	    return sectionLabel;
	}
	
    /**
     * Creates a BookmarkedApiSettings object from current field values.
     * 
     * @return New BookmarkedApiSettings instance
     */
	private BookmarkedApiSettings getCurrentSettings() {
	    return new BookmarkedApiSettings(
	        modelNameEditor.getStringValue(),
	        apiUrlEditor.getStringValue(),
	        apiKeyEditor.getStringValue(),
	        temperatureEditor.getDoubleValue());
	}

    /**
     * Saves the current preference page state.
     * 
     * @return true if save successful, false otherwise
     */
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		try {
			Preferences.saveBookmarkedApiSettings(bookmarkedApiSettings);
		} catch (IOException e) {
			Logger.warning("Failed to save bookmarked API settings: " + e.getMessage());
			return false;
		}
		return result;
	}

	/**
	 * Applies current changes without closing the preference page.
	 */
	@Override
	protected void performApply() {
		performOk(); // Save the preferences immediately on apply.
	}

    /**
     * Restores all preferences to their default values.
     */
	@Override
	protected void performDefaults() {
	    super.performDefaults();
	    bookmarkedApiSettings = Constants.DEFAULT_BOOKMARKED_API_SETTINGS;
	    tableViewer.setInput(bookmarkedApiSettings);
	    tableViewer.refresh();
	}

}