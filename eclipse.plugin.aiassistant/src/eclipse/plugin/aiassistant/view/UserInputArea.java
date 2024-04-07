package eclipse.plugin.aiassistant.view;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.swt.widgets.Text;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;

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
	private Text textArea;
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
		textArea = createTextArea(mainContainer);
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
		return (textArea == null || textArea.isDisposed());
	}

	/**
	 * Retrieves the current text in the text area.
	 *
	 * @return The trimmed text from the text area.
	 */
	public String getText() {
		return textArea.getText().trim();
	}

	/**
	 * Sets the text in the text area.
	 *
	 * @param text The text to be set in the text area.
	 */
	public void setText(String text) {
		Eclipse.runOnUIThreadAsync(() -> textArea.setText(text));
	}

	/**
	 * Sets the focus on the text area.
	 */
	public void setFocus() {
		Eclipse.runOnUIThreadAsync(() -> textArea.setFocus());
	}

	/**
	 * Sets the enabled state of the user input area components.
	 *
	 * @param b True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			textArea.setEnabled(enabled);
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

	/**
	 * Creates the text area component for user input.
	 *
	 * @param parent The parent composite for the text area.
	 * @return The created text area.
	 */
	private Text createTextArea(Composite parent) {
		Text text = new Text(parent, getTextAreaStyle());
		configureTextToolTip(text, INPUT_AREA_TOOLTIP);
		addTraverseListener(text);
		text.setLayoutData(Eclipse.createGridData(true, true));
		return text;
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
				if (textArea.getEnabled()) {
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
				if (textArea.getEnabled()) {
					mainPresenter.onDownArrowClick();
				}
			}
		};
		return Eclipse.createButton(buttonContainer, "", ARROW_DOWN_TOOLTIP, ARROW_DOWN_ICON,
				listener);
	}

	/**
	 * Returns the style flags for the text area component.
	 *
	 * @return The style flags as an integer.
	 */
	private int getTextAreaStyle() {
		return SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
	}

	/**
	 * Configures the tooltip for the given text component.
	 *
	 * @param text        The text component to configure the tooltip for.
	 * @param tooltipText The tooltip text.
	 */
	private void configureTextToolTip(Text text, String tooltipText) {
		if (!Preferences.disableTooltips()) {
			text.setToolTipText(tooltipText);
		}
	}

	/**
	 * Adds a traverse listener to the given text component.
	 *
	 * @param text The text component to add the listener to.
	 */
	private void addTraverseListener(Text text) {
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (textArea.getEnabled() && e.detail == SWT.TRAVERSE_RETURN) {
					handleEnterKeyPress(e.stateMask);
				}
			}
		});
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
