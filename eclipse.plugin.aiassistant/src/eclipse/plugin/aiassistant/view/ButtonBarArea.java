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
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * Manages the button bar with undo, redo, clear, and start/stop buttons.
 */
public class ButtonBarArea {

	// Button configuration constants
	public static final String UNDO_NAME = "Undo";
	public static final String UNDO_TOOLTIP = "Undo the Last Chat Interaction";
	public static final String UNDO_ICON = "Undo.png";

	public static final String REDO_NAME = "Redo";
	public static final String REDO_TOOLTIP = "Redo the Last Undone Chat Interaction";
	public static final String REDO_ICON = "Redo.png";

	public static final String CLEAR_NAME = "Clear";
	public static final String CLEAR_TOOLTIP = "Clear the Chat History";
	public static final String CLEAR_ICON = "Clear.png";

	public static final String START_NAME = "Start";
	public static final String START_ICON = "Start.png";
	public static final String START_TOOLTIP = "Start Response";

	public static final String STOP_NAME = "Stop";
	public static final String STOP_TOOLTIP = "Cancel Response";
	public static final String STOP_ICON = "Stop.png";

	// Core components
	private final MainPresenter mainPresenter;
	private final List<ButtonConfig> buttonConfigs = List.of(
			new ButtonConfig(UNDO_NAME, UNDO_TOOLTIP, UNDO_ICON, this::onUndo),
			new ButtonConfig(REDO_NAME, REDO_TOOLTIP, REDO_ICON, this::onRedo),
			new ButtonConfig(CLEAR_NAME, CLEAR_TOOLTIP, CLEAR_ICON, this::onClear),
			new ButtonConfig(START_NAME, START_TOOLTIP, START_ICON, this::onStartStop));

	// UI components
	private Composite buttonContainer;
	private List<Button> buttons;
	private Button startStopButton;

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
	 * Controls the enabled/disabled state of all buttons in the button bar.
	 *
	 * @param enabled true to enable buttons, false to disable them
	 * @param isRunningJob when true, overrides normal enable/disable behavior for the
	 *                     start/stop button. Instead shows "Stop" state when enabled=false
	 *                     (during processing) and "Start" state when enabled=true (when idle).
	 *                     When false, all buttons including start/stop follow normal enable/disable.
	 */
	public void setInputEnabled(boolean enabled, boolean isRunningJob) {
		Eclipse.runOnUIThreadAsync(() -> {
			for (int i = 0; i < buttons.size(); i++) {
				Button button = buttons.get(i);
				if (isRunningJob && (button.getText().equals(STOP_NAME) || button.getText().equals(START_NAME))) {
					if (!enabled) {
						// Show Stop button when processing - only change if currently Start
						if (button.getText().equals(START_NAME)) {
							button.setText(STOP_NAME);
							button.setToolTipText(STOP_TOOLTIP);
							Eclipse.setButtonIcon(button, STOP_ICON);
						}
						// Stop will be the only control without the SWT.CURSOR_WAIT spinning cursor
						button.setEnabled(true);
						button.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
					} else {
						// Show Start button when idle - only change if currently Stop
						if (button.getText().equals(STOP_NAME)) {
							button.setText(START_NAME);
							button.setToolTipText(START_TOOLTIP);
							Eclipse.setButtonIcon(button, START_ICON);
						}
						button.setEnabled(false);
						button.setCursor(null);
					}
				} else {
					button.setEnabled(enabled);
					button.setCursor(enabled ? null : Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
				}
			}
			if (enabled) {
				updateButtonStates();
			}
		});
	}

	/**
	 * Updates the state of all buttons based on current conditions.
	 * Enables/disables buttons based on conversation state and ensures the
	 * start/stop button shows as "Start" when in idle state.
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
				} else if (button.getText().equals(STOP_NAME) || button.getText().equals(START_NAME)) {
					// Ensure start/stop button shows as "Start" when idle
					if (button.getText().equals(STOP_NAME)) {
						button.setText(START_NAME);
						button.setToolTipText(START_TOOLTIP);
						Eclipse.setButtonIcon(button, START_ICON);
					}
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
	 * Also stores a reference to the start/stop button for later state management.
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

			// Store reference to start/stop button for state management
			if (config.name.equals(START_NAME)) {
				startStopButton = button;
			}
		}
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
	 * Handles the 'Clear' button click event by delegating to the main presenter.
	 */
	private void onClear() {
		mainPresenter.onClear();
	}

	/**
	 * Handles the 'Start/Stop' button click event by delegating to the appropriate main presenter method.
	 * When in "Start" state, sends a default prompt. When in "Stop" state, cancels the current operation.
	 */
	private void onStartStop() {
		if (startStopButton.getText().equals(START_NAME)) {
			mainPresenter.sendPredefinedPrompt(Prompts.DEFAULT);
		} else {
			mainPresenter.onStop();
		}
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