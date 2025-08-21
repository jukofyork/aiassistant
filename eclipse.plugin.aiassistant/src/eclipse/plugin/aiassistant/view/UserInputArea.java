package eclipse.plugin.aiassistant.view;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.preferences.PreferenceConstants;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.SpellCheckedTextBox;

/**
 * This class represents the user input area in the application, which consists
 * of a text area and arrow buttons for navigation.
 */
public class UserInputArea {

	// The height hint we pass to the SpellCheckedTextBox widget
	public static final int HEIGHT_HINT_PIXELS = 80;

	// Adjustment factor, used to try to better match the Browser widget and Eclipse UI font sizes
	public static final int FONT_SIZE_OFFSET = -2;

	public static final String ARROW_UP_TOOLTIP = "Older User Messages";
	public static final String ARROW_DOWN_TOOLTIP = "Newer User Messages";
	public static final String CLEAR_MESSAGES_TOOLTIP = "Clear Message History";
	public static final String SETTINGS_TOOLTIP = "Open the Settings Page";
	public static final String ARROW_UP_ICON = "ArrowUp.png";
	public static final String ARROW_DOWN_ICON = "ArrowDown.png";
	public static final String CLEAR_MESSAGES_ICON = "ClearMessages.png";
	public static final String SETTINGS_ICON = "Settings.png";
	public static final String INPUT_AREA_TOOLTIP = """
			Ctrl+Enter: Delay the Assistant's Response
			Shift+Enter: Insert a Newline""";
	public static final int ARROW_BUTTONS_VERTICAL_SPACING = 0;

	private final MainPresenter mainPresenter;

	private Composite mainContainer;
	private SpellCheckedTextBox spellCheckedTextBox;
	private Composite arrowButtonContainer;
	private Button upArrowButton;
	private Button downArrowButton;
	private Button clearButton;
	private Button settingsButton;

	/**
	 * Constructs a new UserInputArea instance with the given parent composite and
	 * MainPresenter.
	 *
	 * @param parent        The parent composite for this user input area.
	 * @param mainPresenter The presenter responsible for handling user
	 *                      interactions.
	 */
	public UserInputArea(MainPresenter mainPresenter, Composite parent) {
		this.mainPresenter = mainPresenter;
		mainContainer = createMainContainer(parent);
		spellCheckedTextBox = createSpellCheckedTextBox(mainContainer);
		arrowButtonContainer = createArrowButtonContainer(mainContainer);
		upArrowButton = createUpArrowButton(arrowButtonContainer);
		downArrowButton = createDownArrowButton(arrowButtonContainer);
		clearButton = createClearButton(arrowButtonContainer);
		settingsButton = createSettingsButton(arrowButtonContainer);
		setupPropertyChangeListener();
	}

	/**
	 * Checks if this user input area is disposed.
	 *
	 * @return True if the text area is null or disposed, false otherwise.
	 */
	public boolean isDisposed() {
		return spellCheckedTextBox.isDisposed();
	}

	/**
	 * Retrieves the current text in the text area.
	 *
	 * @return The trimmed text from the text area.
	 */
	public String getText() {
		return spellCheckedTextBox.getText();
	}

	/**
	 * Sets the text in the text area.
	 *
	 * @param text The text to be set in the text area.
	 */
	public void setText(String text) {
		spellCheckedTextBox.setText(text);
	}

	/**
	 * Sets the focus on the text area.
	 */
	public void setFocus() {
		spellCheckedTextBox.setFocus();
	}

	/**
	 * Sets the enabled state of the user input area components.
	 *
	 * @param b True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			upArrowButton.setEnabled(enabled);
			downArrowButton.setEnabled(enabled);
			clearButton.setEnabled(enabled);
			settingsButton.setEnabled(enabled);
			if (enabled) {
				updateButtonStates();
			}
			spellCheckedTextBox.setEnabled(enabled);
		});
	}

	/**
	 * Updates the enabled state of all buttons based on the current message history state.
	 * Tests the message history methods to determine button availability without affecting state.
	 */
	public void updateButtonStates() {
		Eclipse.runOnUIThreadAsync(() -> {
			UserMessageHistory messageHistory = mainPresenter.getUserMessageHistory();
			upArrowButton.setEnabled(messageHistory.hasOlderMessages());
			downArrowButton.setEnabled(messageHistory.hasNewerMessages());
			clearButton.setEnabled(!messageHistory.isEmpty());
			// Settings button is always enabled when input is enabled
		});
	}

	/**
	 * Creates the main container for the user input area.
	 *
	 * @param parent The parent composite for the main container.
	 * @return The created composite.
	 */
	private Composite createMainContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(2, false, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, Constants.DEFAULT_INTERNAL_SPACING, -1));
		container.setLayoutData(Eclipse.createGridData(true, false));
		return container;
	}

	/**
	 * Creates and configures a {@link SpellCheckedTextBox} for the user input area.
	 * This text box includes spell-checking capabilities and is configured to
	 * handle special key press events for message sending and text manipulation.
	 *
	 * @param parent The parent composite where this text box will be placed. Must
	 *               not be null.
	 * @return A fully configured {@link SpellCheckedTextBox} ready for user
	 *         interaction.
	 */
	private SpellCheckedTextBox createSpellCheckedTextBox(Composite parent) {
		SpellCheckedTextBox spellCheckedTextBox = new SpellCheckedTextBox(parent, HEIGHT_HINT_PIXELS,
				Preferences.getChatFontSize() + FONT_SIZE_OFFSET, this::handleEnterKeyPress);
		spellCheckedTextBox.configureTextToolTip(INPUT_AREA_TOOLTIP);
		return spellCheckedTextBox;
	}

	/**
	 * Creates the container for arrow buttons.
	 *
	 * @param parent The parent composite for the arrow button container.
	 * @return The created composite.
	 */
	private Composite createArrowButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(Eclipse.createGridLayout(1, true, Constants.DEFAULT_EXTERNAL_MARGINS,
				Constants.DEFAULT_EXTERNAL_MARGINS, -1, ARROW_BUTTONS_VERTICAL_SPACING));
		container.setLayoutData(Eclipse.createGridData(false, false));
		return container;
	}

	/**
	 * Creates the up arrow button component.
	 *
	 * @param buttonContainer The parent composite for the up arrow button.
	 * @return The created button.
	 */
	private Button createUpArrowButton(Composite buttonContainer) {
		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (spellCheckedTextBox.getEnabled()) {
					mainPresenter.onUpArrowClick();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", ARROW_UP_TOOLTIP, ARROW_UP_ICON, listener);
	}

	/**
	 * Creates the down arrow button component.
	 *
	 * @param buttonContainer The parent composite for the down arrow button.
	 * @return The created button.
	 */
	private Button createDownArrowButton(Composite buttonContainer) {
		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (spellCheckedTextBox.getEnabled()) {
					mainPresenter.onDownArrowClick();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", ARROW_DOWN_TOOLTIP, ARROW_DOWN_ICON, listener);
	}

	/**
	 * Creates the clear messages button component.
	 *
	 * @param buttonContainer The parent composite for the clear button.
	 * @return The created button.
	 */
	private Button createClearButton(Composite buttonContainer) {
		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (spellCheckedTextBox.getEnabled()) {
					mainPresenter.onAttemptClearMessages();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", CLEAR_MESSAGES_TOOLTIP, CLEAR_MESSAGES_ICON, listener);
	}

	/**
	 * Creates the settings button component.
	 *
	 * @param buttonContainer The parent composite for the settings button.
	 * @return The created button.
	 */
	private Button createSettingsButton(Composite buttonContainer) {
		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (spellCheckedTextBox.getEnabled()) {
					onSettings();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", SETTINGS_TOOLTIP, SETTINGS_ICON, listener);
	}

	/**
	 * Registers a property change listener to handle changes in font size preferences.
	 * This listener reacts to changes in chat font size by updating the input area font immediately.
	 */
	private void setupPropertyChangeListener() {
		Preferences.getDefault().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.CHAT_FONT_SIZE)) {
					spellCheckedTextBox.setFontSize(Preferences.getChatFontSize() + FONT_SIZE_OFFSET);
				}
			}
		});
	}

	/**
	 * Handles the Enter key press in the text area component.
	 * NOTE: The Enter key is overloaded and has three different functions:
	 * - Shift+Enter : Insert newline (default Eclipse behaviour?)
	 * - Ctrl+Enter : Send message, but don't schedule a reply yet.
	 * - Enter : Send message and schedule a reply.
	 *
	 * @param stateMask The state mask of the key event.
	 */
	private void handleEnterKeyPress(int stateMask) {
		if ((stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
			mainPresenter.sendPredefinedPrompt(Prompts.DEFAULT_DELAYED);
		} else if ((stateMask & SWT.MODIFIER_MASK) != SWT.MOD2) {
			mainPresenter.sendPredefinedPrompt(Prompts.DEFAULT);
		}
	}

	/**
	 * Opens the Preferences dialog when the 'Settings' button is clicked.
	 */
	private void onSettings() {
		Preferences.openPreferenceDialog();
	}

}