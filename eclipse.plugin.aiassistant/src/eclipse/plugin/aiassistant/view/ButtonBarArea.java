package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class represents the button bar area in the application. It contains
 * methods to create and manage buttons, as well as handle user interactions
 * with them.
 */
public class ButtonBarArea {

	public static final String STOP_NAME = "Stop";
	public static final String STOP_TOOLTIP = "Cancel the AI Response";
	public static final String STOP_ICON = "Stop.png";
	public static final String CLEAR_NAME = "Clear";
	public static final String CLEAR_TOOLTIP = "Clear the Chat History";
	public static final String CLEAR_ICON = "Clear.png";
	public static final String UNDO_NAME = "Undo";
	public static final String UNDO_TOOLTIP = "Undo the Last Chat Interaction";
	public static final String UNDO_ICON = "Undo.png";
	public static final String REDO_NAME = "Redo";
	public static final String REDO_TOOLTIP = "Redo the Last Undone Chat Interaction";
	public static final String REDO_ICON = "Redo.png";
	public static final String IMPORT_NAME = "Import";
	public static final String IMPORT_TOOLTIP = "Import Chat History (JSON)";
	public static final String IMPORT_ICON = "Import.png";
	public static final String EXPORT_NAME = "Export";
	public static final String EXPORT_TOOLTIP = "Export Chat History (JSON or Markdown)";
	public static final String EXPORT_ICON = "Export.png";
	public static final String SETTINGS_NAME = "Settings";
	public static final String SETTINGS_TOOLTIP = "Open the Settings Page";
	public static final String SETTINGS_ICON = "Settings.png";

	private final MainPresenter mainPresenter;

	private final List<ButtonConfig> buttonConfigs = List.of(
			new ButtonConfig(STOP_NAME, STOP_TOOLTIP, STOP_ICON, this::onStop),
			new ButtonConfig(CLEAR_NAME, CLEAR_TOOLTIP, CLEAR_ICON, this::onClear),
			new ButtonConfig(UNDO_NAME, UNDO_TOOLTIP, UNDO_ICON, this::onUndo),
			new ButtonConfig(REDO_NAME, REDO_TOOLTIP, REDO_ICON, this::onRedo),
			new ButtonConfig(IMPORT_NAME, IMPORT_TOOLTIP, IMPORT_ICON, this::onImport),
			new ButtonConfig(EXPORT_NAME, EXPORT_TOOLTIP, EXPORT_ICON, this::onExport),
			new ButtonConfig(SETTINGS_NAME, SETTINGS_TOOLTIP, SETTINGS_ICON,
					this::onSettings));

	private Composite buttonContainer;
	private List<Button> buttons;

	/**
	 * Constructs a new ButtonBarArea instance with the given main presenter and
	 * parent composite.
	 *
	 * @param mainPresenter The main presenter for the application.
	 * @param parent        The parent composite for the button container.
	 */
	public ButtonBarArea(MainPresenter mainPresenter, Composite parent) {
		this.mainPresenter = mainPresenter;
		buttonContainer = createButtonContainer(parent);
		createButtons();
	}

	/**
	 * Sets the input enabled state for all buttons in the button bar area.
	 *
	 * @param enabled True to enable input, false to disable it.
	 */
	public void setInputEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			buttonContainer.setCursor(enabled ? null : Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
			for (int i = 0; i < buttons.size(); i++) {
				Button button = buttons.get(i);
				if (button.getText().equals(STOP_NAME)) {
					button.setEnabled(!enabled); // Stop has opposite enabled/disabled status to other buttons.
					button.setCursor(enabled ? null : Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
				} else {
					button.setEnabled(enabled);
				}
			}
			if (enabled) {
				updateButtonStates();
			}
		});
	}

	/**
	 * Updates the state of all buttons based on current conditions.
	 */
	public void updateButtonStates() {
		Eclipse.runOnUIThreadAsync(() -> {
			for (int i = 0; i < buttons.size(); i++) {
				Button button = buttons.get(i);
				if (button.getText().equals(UNDO_NAME)) {
					button.setEnabled(!mainPresenter.isConversationEmpty());
				} else if (button.getText().equals(REDO_NAME)) {
					button.setEnabled(mainPresenter.canRedo());
				} else if (button.getText().equals(CLEAR_NAME)) {
					button.setEnabled(!mainPresenter.isConversationEmpty());
				} else if (button.getText().equals(EXPORT_NAME)) {
					button.setEnabled(!mainPresenter.isConversationEmpty());
				}
			}
		});
	}

	/**
	 * Creates a new Composite to contain buttons in the button bar area.
	 *
	 * @param parent The parent composite for the button container.
	 * @return The newly created button container composite.
	 */
	private Composite createButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(buttonConfigs.size(), true, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, Constants.DEFAULT_INTERNAL_SPACING, -1));
		container.setLayoutData(Eclipse.createGridData(true, false));
		return container;
	}

	/**
	 * Creates and initializes buttons based on the provided button configurations.
	 */
	private void createButtons() {
		buttons = new ArrayList<>();
		for (ButtonConfig config : buttonConfigs) {
			Button button = Eclipse.createButton(buttonContainer, config.name, config.tooltip, config.filename,
					new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					config.action.run();
				}
			});
			buttons.add(button);
		}
	}

	/**
	 * Handles the 'Stop' button click event by delegating to the main presenter.
	 */
	private void onStop() {
		mainPresenter.onStop();
	}

	/**
	 * Handles the 'Clear' button click event by delegating to the main presenter.
	 */
	private void onClear() {
		mainPresenter.onClear();
	}

	/**
	 * Handles the 'Undo' button click event by delegating to the main presenter.
	 */
	private void onUndo() {
		mainPresenter.onUndo();
	}

	/**
	 * Handles the 'Redo' button click event by delegating to the main presenter.
	 */
	private void onRedo() {
		mainPresenter.onRedo();
	}

	/**
	 * Handles the 'Import' button click event by delegating to the main presenter.
	 */
	private void onImport() {
		mainPresenter.onImport();
	}

	/**
	 * Handles the 'Export' button click event by delegating to the main presenter.
	 */
	private void onExport() {
		mainPresenter.onExport();
	}

	/**
	 * Opens the Preferences dialog when the 'Settings' button is clicked.
	 */
	private void onSettings() {
		Preferences.openPreferenceDialog();
	}

	/**
	 * Represents a button configuration with its name, tooltip, icon filename, and
	 * action.
	 */
	private static class ButtonConfig {
		private final String name;
		private final String tooltip;
		private final String filename;
		private final Runnable action;

		/**
		 * Constructs a new ButtonConfig instance with the given parameters.
		 *
		 * @param name     The button's name or label.
		 * @param tooltip  The button's tooltip text.
		 * @param filename The filename of the button's icon.
		 * @param action   The action to be performed when the button is clicked.
		 */
		public ButtonConfig(String name, String tooltip, String filename, Runnable action) {
			this.name = name;
			this.tooltip = tooltip;
			this.filename = filename;
			this.action = action;
		}
	}

}