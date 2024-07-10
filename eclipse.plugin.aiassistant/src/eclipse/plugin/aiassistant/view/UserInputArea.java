package eclipse.plugin.aiassistant.view;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.SpellCheckedTextBox;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * This class represents the user input area in the application, which consists
 * of a text area and arrow buttons for navigation.
 */
public class UserInputArea {
	
	public static final String ARROW_UP_TOOLTIP = "Older User Messages";
	public static final String ARROW_DOWN_TOOLTIP = "Newer User Messages";
	public static final String ARROW_UP_ICON = "ArrowUp.png";
	public static final String ARROW_DOWN_ICON = "ArrowDown.png";
	public static final String INPUT_AREA_TOOLTIP = """
			   										Ctrl+Enter: Delay the Assistant's Response
													Shift+Enter: Insert a Newline""";
	public static final int ARROW_BUTTONS_VERTICAL_SPACING = 0;

	private final MainPresenter mainPresenter;

	private Composite mainContainer;
    private SpellCheckedTextBox spellCheckingEditor;
	private Composite arrowButtonContainer;
	private Button upArrowButton;
	private Button downArrowButton;

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
		spellCheckingEditor = createSpellCheckingEditor(mainContainer);
		arrowButtonContainer = createArrowButtonContainer(mainContainer);
		upArrowButton = createUpArrowButton(arrowButtonContainer);
		downArrowButton = createDownArrowButton(arrowButtonContainer);
	}

	/**
	 * Checks if this user input area is disposed.
	 *
	 * @return True if the text area is null or disposed, false otherwise.
	 */
	public boolean isDisposed() {
		return spellCheckingEditor.isDisposed();
	}

	/**
	 * Retrieves the current text in the text area.
	 *
	 * @return The trimmed text from the text area.
	 */
	public String getText() {
		return spellCheckingEditor.getText();
	}

	/**
	 * Sets the text in the text area.
	 *
	 * @param text The text to be set in the text area.
	 */
	public void setText(String text) {
		spellCheckingEditor.setText(text);;
	}

	/**
	 * Sets the focus on the text area.
	 */
	public void setFocus() {
		spellCheckingEditor.setFocus();
	}

	/**
	 * Sets the enabled state of the user input area components.
	 *
	 * @param b True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			spellCheckingEditor.setEnabled(enabled);
			upArrowButton.setEnabled(enabled);
			downArrowButton.setEnabled(enabled);
			mainContainer.setCursor(enabled ? null : Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
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
	
	private SpellCheckedTextBox createSpellCheckingEditor(Composite parent) {
	    SpellCheckedTextBox spellCheckingEditor = new SpellCheckedTextBox(parent, this::handleEnterKeyPress);
	    spellCheckingEditor.configureTextToolTip(INPUT_AREA_TOOLTIP);    
	    return spellCheckingEditor;
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
				if (spellCheckingEditor.getEnabled()) {
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
				if (spellCheckingEditor.getEnabled()) {
					mainPresenter.onDownArrowClick();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", ARROW_DOWN_TOOLTIP, ARROW_DOWN_ICON,
				listener);
	}

	/**
	 * Handles the Enter key press in the text area component. 
	 * NOTE: The Enter key is overloaded and has three different functions:
	 *       - Shift+Enter : Insert newline (default Eclipse behaviour?)
	 *       - Ctrl+Enter : Send message, but don't schedule a reply yet.
	 *       - Enter : Send message and schedule a reply.
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

}
