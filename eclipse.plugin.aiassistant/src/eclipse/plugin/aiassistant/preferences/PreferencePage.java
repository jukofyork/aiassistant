package eclipse.plugin.aiassistant.preferences;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.network.OpenAiApiClient;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.JsonFieldEditor;
import eclipse.plugin.aiassistant.utility.PasswordFieldEditor;
import eclipse.plugin.aiassistant.utility.UrlFieldEditor;

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

	/**
	 * Extension of BooleanFieldEditor that provides access to the underlying checkbox control.
	 * This allows attaching listeners to detect user interactions with the checkbox.
	 */
	public class AccessibleBooleanFieldEditor extends BooleanFieldEditor {

		private Composite parent = null;

		public AccessibleBooleanFieldEditor(String name, String label, int style, Composite parent) {
			super(name, label, style, parent);
			this.parent = parent;
		}

		public AccessibleBooleanFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			this.parent = parent;
		}

		/**
		 * Returns the checkbox control by exposing the protected getChangeControl method.
		 */
		public Button getCheckboxControl(Composite parent) {
			return super.getChangeControl(parent);
		}

		public void setBooleanValue(boolean value) {
			boolean oldValue = getBooleanValue();
			if (oldValue != value) {
				getCheckboxControl(this.parent).setSelection(value);
				valueChanged(oldValue, value);
			}
		}

	}

	/** Field editors for general plugin settings */
	private IntegerFieldEditor connectionTimeoutEditor;
	private IntegerFieldEditor requestTimeoutEditor;
	private IntegerFieldEditor streamingUpdateIntervalEditor;
	private IntegerFieldEditor chatFontSizeEditor;
	private IntegerFieldEditor notificationFontSizeEditor;
	private BooleanFieldEditor disableTooltipsEditor;

	/** Field editors for current API configuration */
	private StringFieldEditor modelNameEditor;
	private UrlFieldEditor apiUrlEditor;
	private PasswordFieldEditor apiKeyEditor;
	private JsonFieldEditor jsonOverridesEditor;
	private AccessibleBooleanFieldEditor useStreamingEditor;
	private AccessibleBooleanFieldEditor useSystemMessageEditor;
	private AccessibleBooleanFieldEditor useDeveloperMessageEditor;

	private Button bookmarkButton;
	private Button unbookmarkButton;
	private Button validateButton;

	private Button clearButton;
	private Button sortButton;

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
		createCurrentApiSettingsButtons(parent);

		createSectionHeader(parent, "\nBOOKMARKED API SETTINGS:");
		createTable(parent);
		createTableActionButtons(parent);

		// Add listeners to all fields that affect bookmark status
		addSettingsChangeListener(modelNameEditor.getTextControl(parent));
		addSettingsChangeListener(apiUrlEditor.getTextControl(parent));
		addSettingsChangeListener(apiKeyEditor.getTextControl(parent));
		addSettingsChangeListener(jsonOverridesEditor.getTextControl(parent));
		addSettingsChangeListener(useStreamingEditor.getCheckboxControl(parent));
		addSettingsChangeListener(useSystemMessageEditor.getCheckboxControl(parent));
		addSettingsChangeListener(useDeveloperMessageEditor.getCheckboxControl(parent));

		// Add mutual exclusion logic for system and developer message checkboxes
		addMutualExclusionLogic(parent);
	}

	/**
	 * Initializes the preference page and sets the initial visibility
	 * of the password field after its value has been loaded.
	 */
	@Override
	protected void initialize() {
		super.initialize();
		if (apiKeyEditor != null) {
			apiKeyEditor.setPasswordVisible(apiKeyEditor.getStringValue().isBlank());
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		updateBookmarkButtonsVisibility();
	}

	/**
	 * Creates the general settings section with timeout and display preferences.
	 *
	 * @param parent The parent composite where the fields will be created
	 */
	private void createGlobalSettingsGroup(Composite parent) {
		connectionTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.CONNECTION_TIMEOUT,
				"Connection Timeout (s):", parent);
		connectionTimeoutEditor.setValidRange(Constants.MIN_CONNECTION_TIMEOUT, Constants.MAX_CONNECTION_TIMEOUT);

		requestTimeoutEditor = new IntegerFieldEditor(PreferenceConstants.REQUEST_TIMEOUT,
				"Request Timeout (s):", parent);
		requestTimeoutEditor.setValidRange(Constants.MIN_REQUEST_TIMEOUT, Constants.MAX_REQUEST_TIMEOUT);

		chatFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.CHAT_FONT_SIZE, "Chat Font Size:", parent);
		chatFontSizeEditor.setValidRange(Constants.MIN_CHAT_FONT_SIZE, Constants.MAX_CHAT_FONT_SIZE);

		notificationFontSizeEditor = new IntegerFieldEditor(PreferenceConstants.NOTIFICATION_FONT_SIZE,
				"Notification Font Size:", parent);
		notificationFontSizeEditor.setValidRange(Constants.MIN_NOTIFICATION_FONT_SIZE,
				Constants.MAX_NOTIFICATION_FONT_SIZE);

		streamingUpdateIntervalEditor = new IntegerFieldEditor(PreferenceConstants.STREAMING_UPDATE_INTERVAL,
				"Streaming Interval (ms):", parent);
		streamingUpdateIntervalEditor.setValidRange(Constants.MIN_STREAMING_UPDATE_INTERVAL, Constants.MAX_STREAMING_UPDATE_INTERVAL);

		disableTooltipsEditor = new BooleanFieldEditor(PreferenceConstants.DISABLE_TOOLTIPS, "Disable Tooltips",
				BooleanFieldEditor.SEPARATE_LABEL, parent);

		addField(connectionTimeoutEditor);
		addField(requestTimeoutEditor);
		addField(chatFontSizeEditor);
		addField(notificationFontSizeEditor);
		addField(streamingUpdateIntervalEditor);
		addField(disableTooltipsEditor);
	}

	/**
	 * Creates the current API settings section for configuring the active API connection.
	 *
	 * @param parent The parent composite where the fields will be created
	 */
	private void createCurrentApiSettingsGroup(Composite parent) {
		modelNameEditor = new StringFieldEditor(PreferenceConstants.CURRENT_MODEL_NAME, "Model Name:", parent);
		modelNameEditor.setEmptyStringAllowed(false);

		apiUrlEditor = new UrlFieldEditor(PreferenceConstants.CURRENT_API_URL, "API URL:", parent);
		apiUrlEditor.setEmptyStringAllowed(false);

		// NOTE: The API-key is allowed to be blank or empty for use with llama.cpp or other local back-ends
		apiKeyEditor = new PasswordFieldEditor(PreferenceConstants.CURRENT_API_KEY, "API Key:", parent);
		apiKeyEditor.setEmptyStringAllowed(true);

		jsonOverridesEditor = new JsonFieldEditor(PreferenceConstants.CURRENT_JSON_OVERRIDES, "JSON Overrides:", parent);
		jsonOverridesEditor.setEmptyStringAllowed(true);

		useStreamingEditor = new AccessibleBooleanFieldEditor(PreferenceConstants.CURRENT_USE_STREAMING,
				"Use Streaming", BooleanFieldEditor.SEPARATE_LABEL, parent);

		useSystemMessageEditor = new AccessibleBooleanFieldEditor(PreferenceConstants.CURRENT_USE_SYSTEM_MESSAGE,
				"System Message", BooleanFieldEditor.SEPARATE_LABEL, parent);

		useDeveloperMessageEditor = new AccessibleBooleanFieldEditor(PreferenceConstants.CURRENT_USE_DEVELOPER_MESSAGE,
				"Developer Message", BooleanFieldEditor.SEPARATE_LABEL, parent);

		addField(modelNameEditor);
		addField(apiUrlEditor);
		addField(apiKeyEditor);
		addField(jsonOverridesEditor);
		addField(useStreamingEditor);
		addField(useSystemMessageEditor);
		addField(useDeveloperMessageEditor);
	}

	/**
	 * Adds mutual exclusion logic to the system and developer message checkboxes.
	 * When one is checked, the other is automatically unchecked.
	 *
	 * @param parent The parent composite
	 */
	private void addMutualExclusionLogic(Composite parent) {
		Button systemMessageCheckbox = useSystemMessageEditor.getCheckboxControl(parent);
		Button developerMessageCheckbox = useDeveloperMessageEditor.getCheckboxControl(parent);

		systemMessageCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (systemMessageCheckbox.getSelection()) {
					useDeveloperMessageEditor.setBooleanValue(false);
				}
			}
		});

		developerMessageCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (developerMessageCheckbox.getSelection()) {
					useSystemMessageEditor.setBooleanValue(false);
				}
			}
		});
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
		scrolledData.heightHint = 400;
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
		createTableColumn("Model Name", 22, SWT.LEFT, tableLayout, e -> ((BookmarkedApiSettings) e).getModelName());
		createTableColumn("API URL", 22, SWT.LEFT, tableLayout, e -> ((BookmarkedApiSettings) e).getApiUrl());
		createTableColumn("JSON Overrides", 26, SWT.LEFT, tableLayout,
				e -> ((BookmarkedApiSettings) e).getJsonOverrides());
		createTableColumn("Streaming", 10, SWT.CENTER, tableLayout,
				e -> ((BookmarkedApiSettings) e).getUseStreaming() ? "🗹" : "☐");
		createTableColumn("System", 10, SWT.CENTER, tableLayout,
				e -> ((BookmarkedApiSettings) e).getUseSystemMessage() ? "🗹" : "☐");
		createTableColumn("Developer", 10, SWT.CENTER, tableLayout,
				e -> ((BookmarkedApiSettings) e).getUseDeveloperMessage() ? "🗹" : "☐");

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items.length > 0) {
					BookmarkedApiSettings selectedSettings = (BookmarkedApiSettings) items[0].getData();
					modelNameEditor.setStringValue(selectedSettings.getModelName());
					apiUrlEditor.setStringValue(selectedSettings.getApiUrl());
					apiKeyEditor.setStringValue(selectedSettings.getApiKey());
					apiKeyEditor.setPasswordVisible(apiKeyEditor.getStringValue().isBlank()); // Hide if not blank for safety
					jsonOverridesEditor.setStringValue(selectedSettings.getJsonOverrides());
					useStreamingEditor.setBooleanValue(selectedSettings.getUseStreaming());
					useSystemMessageEditor.setBooleanValue(selectedSettings.getUseSystemMessage());
					useDeveloperMessageEditor.setBooleanValue(selectedSettings.getUseDeveloperMessage());
					updateBookmarkButtonsVisibility();
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
	 * Creates and configures the action buttons for managing the current API settings.
	 * Sets up a button composite with three main actions:
	 * - Bookmark: Saves current API configuration if valid and unique
	 * - Unbookmark: Removes matching API configuration from bookmarks
	 * - Validate: Tests API connection and verifies model availability
	 *
	 * @param parent The parent composite where the buttons will be created
	 * @see BookmarkedApiSettings
	 * @see OpenAiApiClient#getApiStatus(String, String)
	 */
	private void createCurrentApiSettingsButtons(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(10, true);
		buttonComposite.setLayout(layout);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		buttonComposite.setLayoutData(gridData);

		bookmarkButton = createActionButton(buttonComposite, "Bookmark", "Bookmark current API settings", "Bookmark.png", () -> {
			BookmarkedApiSettings currentSettings = getCurrentSettings();
			if (currentSettings != null && !bookmarkedApiSettings.contains(currentSettings)) {
				bookmarkedApiSettings.add(currentSettings);
				tableViewer.refresh();
				updateBookmarkButtonsVisibility();
			}
		});

		unbookmarkButton = createActionButton(buttonComposite, "Unbookmark", "Unbookmark matching API settings", "Unbookmark.png", () -> {
			BookmarkedApiSettings currentSettings = getCurrentSettings();
			if (currentSettings != null) {
				bookmarkedApiSettings.remove(currentSettings);
				tableViewer.refresh();
				updateBookmarkButtonsVisibility();
			}
		});

		validateButton = createActionButton(buttonComposite, "Validate", "Validate current API settings", "Refresh.png", () -> {
			String currentModelName = modelNameEditor.getStringValue().trim();
			String currentApiUrl = apiUrlEditor.getStringValue().trim();
			String currentApiKey = apiKeyEditor.getStringValue().trim();
			if (OpenAiApiClient.getApiStatus(currentApiUrl, currentApiKey).equals("OK")) {
				java.util.List<String> modelNames = OpenAiApiClient.fetchAvailableModelNames(currentApiUrl,
						currentApiKey);
				if (modelNames.contains(currentModelName)) {
					Eclipse.runOnUIThreadAsync(() -> {
						Eclipse.setButtonIcon(validateButton, "Pass.png");
					});
				}
				else {
					Eclipse.runOnUIThreadAsync(() -> {
						Eclipse.setButtonIcon(validateButton, "Exclamation.png");
					});
				}
			}
		});

	}

	/**
	 * Creates and configures the action buttons for managing the bookmarked API settings table.
	 * Implements three main operations:
	 * - Clear: Removes all bookmarked settings
	 * - Sort: Alphabetically sorts bookmarks by model name
	 * - Populate: Automatically discovers and bookmarks compatible API models
	 *
	 * The populate operation filters available models based on the current model name
	 * as a substring match, allowing for partial name searches.
	 *
	 * @param parent The parent composite where the buttons will be created
	 * @see BookmarkedApiSettings#compareTo(BookmarkedApiSettings)
	 * @see OpenAiApiClient#fetchAvailableModelNames(String, String)
	 */
	private void createTableActionButtons(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(10, true);
		buttonComposite.setLayout(layout);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		buttonComposite.setLayoutData(gridData);

		// Clear button
		clearButton = createActionButton(buttonComposite, "Clear", "Clear all bookmarked API settings", "Clear.png", () -> {
			bookmarkedApiSettings.clear();
			tableViewer.refresh();
			updateBookmarkButtonsVisibility();
		});

		// Sort button
		sortButton = createActionButton(buttonComposite, "Sort", "Sort bookmarked API settings", "Sort.png", () -> {
			bookmarkedApiSettings.sort(null);
			tableViewer.refresh();
			updateBookmarkButtonsVisibility();
		});

		// Populate button
		createActionButton(buttonComposite, "Populate", "Populate API settings", "Populate.png", () -> {
			String currentModelName = modelNameEditor.getStringValue().trim();
			String currentApiUrl = apiUrlEditor.getStringValue().trim();
			String currentApiKey = apiKeyEditor.getStringValue().trim();
			String currentJsonOverrides = jsonOverridesEditor.getStringValue();
			boolean currentUseStreaming = useStreamingEditor.getBooleanValue();
			boolean currentUseSystemMessage = useSystemMessageEditor.getBooleanValue();
			boolean currentUseDeveloperMessage = useDeveloperMessageEditor.getBooleanValue();
			if (OpenAiApiClient.getApiStatus(currentApiUrl, currentApiKey).equals("OK")) {
				java.util.List<String> modelNames = OpenAiApiClient.fetchAvailableModelNames(currentApiUrl,
						currentApiKey);
				for (String modelName : modelNames) {
					// If current model name is a substring of model name (or empty)
					if (modelName.toLowerCase().contains(currentModelName.toLowerCase())) {
						BookmarkedApiSettings newSetting = new BookmarkedApiSettings(modelName, currentApiUrl,
								currentApiKey, currentJsonOverrides, currentUseStreaming, currentUseSystemMessage, currentUseDeveloperMessage);
						if (!bookmarkedApiSettings.contains(newSetting)) {
							bookmarkedApiSettings.add(newSetting);
						}
					}
				}
				tableViewer.refresh();
				updateBookmarkButtonsVisibility();
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
	private Button createActionButton(Composite parent, String text, String tooltip, String imageName, Runnable action) {
		return Eclipse.createButton(parent, text, tooltip, imageName,
				new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				action.run();
				updateBookmarkButtonsVisibility();
			}
		});
	}

	/**
	 * Attaches a listener to a text control that triggers button state updates
	 * whenever the text content changes. This ensures bookmark-related buttons
	 * stay in sync with the current input state.
	 *
	 * @param textControl The SWT Text widget to monitor for changes
	 * @see org.eclipse.swt.widgets.Text
	 * @see #updateBookmarkButtonsVisibility()
	 */
	private void addSettingsChangeListener(Text textControl) {
		textControl.addModifyListener(e -> updateBookmarkButtonsVisibility());
	}

	/**
	 * Attaches a listener to a button control that triggers button state updates
	 * whenever its selection state changes. This ensures bookmark-related buttons
	 * stay in sync with the current input state.
	 *
	 * @param buttonControl The SWT Button widget to monitor for changes
	 * @see org.eclipse.swt.widgets.Button
	 * @see #updateBookmarkButtonsVisibility()
	 */
	private void addSettingsChangeListener(Button buttonControl) {
		buttonControl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateBookmarkButtonsVisibility();
			}
		});
	}

	/**
	 * Updates the enabled state and icons of all bookmark-related buttons based on
	 * the current API settings and bookmark state. The update happens in two phases:
	 *
	 * 1. Synchronously resets the validate button icon to prevent stale states
	 * 2. Asynchronously updates all button states to prevent UI freezing
	 *
	 * Button states are determined by:
	 * - Validate: Enabled if current settings are valid
	 * - Bookmark: Enabled if settings are valid and not already bookmarked
	 * - Unbookmark: Enabled if settings are valid and currently bookmarked
	 * - Clear/Sort: Enabled if any bookmarks exist
	 *
	 * @see Eclipse#runOnUIThreadSync(Runnable)
	 * @see Eclipse#runOnUIThreadAsync(Runnable)
	 * @see #getCurrentSettings()
	 */
	private void updateBookmarkButtonsVisibility() {
		boolean isValidatable;
		boolean isBookmarkable;
		boolean isUnbookmarkable;
		BookmarkedApiSettings currentSettings = getCurrentSettings();
		if (currentSettings == null) {
			isValidatable = false;
			isBookmarkable = false;
			isUnbookmarkable = false;
		}
		else {
			isValidatable = true;
			isBookmarkable = !bookmarkedApiSettings.contains(getCurrentSettings());
			isUnbookmarkable = bookmarkedApiSettings.contains(getCurrentSettings());
		}

		// Reset validate button icon synchronously to ensure immediate feedback
		Eclipse.runOnUIThreadSync(() -> {
			Eclipse.setButtonIcon(validateButton, "Refresh.png");
		});

		// Update button states asynchronously to maintain UI responsiveness
		Eclipse.runOnUIThreadAsync(() -> {
			bookmarkButton.setEnabled(isBookmarkable);
			unbookmarkButton.setEnabled(isUnbookmarkable);
			validateButton.setEnabled(isValidatable);
			clearButton.setEnabled(!bookmarkedApiSettings.isEmpty());
			sortButton.setEnabled(!bookmarkedApiSettings.isEmpty());
		});
	}

	/**
	 * Creates a table column with specified configuration.
	 *
	 * @param title Column header text
	 * @param weight Column width weight
	 * @param alignment Column alignment (e.g., SWT.LEFT, SWT.CENTER, SWT.RIGHT)
	 * @param tableLayout Layout manager for the table
	 * @param labelProvider Function to extract display text from table elements
	 */
	private void createTableColumn(String title, int weight, int alignment, TableColumnLayout tableLayout,
			Function<Object, String> labelProvider) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(alignment);
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
	 * Creates a BookmarkedApiSettings object from the current field values using
	 * a two-phase validation process:
	 *
	 * 1. Validates all string field editor states
	 * 2. Verifies non-blank content for all string fields
	 *
	 * All validations must be performed on the UI thread since they access SWT widgets.
	 * An AtomicBoolean is used to safely return values from UI thread operations.
	 *
	 * @return A new BookmarkedApiSettings instance if all validations pass,
	 *         or null if any validation fails or fields are blank
	 * @see BookmarkedApiSettings
	 * @see Eclipse#runOnUIThreadSync(Runnable)
	 * @see org.eclipse.jface.preference.FieldEditor#isValid()
	 */
	private BookmarkedApiSettings getCurrentSettings() {
		AtomicBoolean allValid = new AtomicBoolean();

		// Validate string field editor states
		Eclipse.runOnUIThreadSync(() -> {
			allValid.set(modelNameEditor.isValid()
					&& apiUrlEditor.isValid()
					&& apiKeyEditor.isValid()
					&& jsonOverridesEditor.isValid());
		});

		if (allValid.get()) {
			// Verify non-blank content for all required string fields
			// NOTE: The API-key and JSON overrides are allowed to be blank or empty
			Eclipse.runOnUIThreadSync(() -> {
				allValid.set(!modelNameEditor.getStringValue().isBlank()
						&& !apiUrlEditor.getStringValue().isBlank());
			});

			if (allValid.get()) {
				return new BookmarkedApiSettings(
						modelNameEditor.getStringValue(),
						apiUrlEditor.getStringValue(),
						apiKeyEditor.getStringValue(),
						jsonOverridesEditor.getStringValue(),
						useStreamingEditor.getBooleanValue(),
						useSystemMessageEditor.getBooleanValue(),
						useDeveloperMessageEditor.getBooleanValue());
			}
		}
		return null;
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
		updateBookmarkButtonsVisibility();
	}

}