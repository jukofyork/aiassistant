package eclipse.plugin.aiassistant.utility;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A field editor for password type preferences that masks input with bullet characters.
 *
 * This class extends {@link StringFieldEditor} to provide secure password entry with
 * a toggle button that allows showing/hiding the password text. When hidden, the password
 * field is also non-editable to prevent accidental modification.
 *
 * @see StringFieldEditor
 */
public class PasswordFieldEditor extends StringFieldEditor {

	/** Composite container for the text field and show/hide button */
	private Composite textButtonComposite;

	/** Button to toggle password visibility */
	private Button showPasswordButton;

	/** Flag indicating whether the password is currently visible */
	private boolean showPassword = false;

	/**
	 * Creates a new password field editor with no initialization.
	 *
	 * This constructor is intended for use by subclasses only.
	 */
	protected PasswordFieldEditor() {
	}

	/**
	 * Creates a password field editor with the given parameters.
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent composite where this field editor will be placed
	 */
	public PasswordFieldEditor(String name, String labelText, Composite parent) {
		init(name, labelText);
		createControl(parent);
	}

	/**
	 * Sets the visibility state of the password text.
	 *
	 * This method sets the internal flag to show or hide the password and updates the
	 * UI controls accordingly. If the password field is currently empty,
	 * it will remain shown and editable as per the standard behavior, regardless
	 * of the requested visibility state, but the internal state will be set
	 * for the next time text is entered.
	 *
	 * @param visible {@code true} to show the password, {@code false} to hide it.
	 */
	public void setPasswordVisible(boolean visible) {
		// Set the desired state based on the parameter
		showPassword = visible;
		// Update the UI controls, which will respect the empty field logic
		// (i.e., if empty, it will force showPassword = true internally in updateControlStates)
		updateControlStates();
	}

	/**
	 * Returns the number of controls managed by this field editor.
	 *
	 * This implementation returns 2 for the label and the composite containing
	 * both the text field and the show/hide button.
	 *
	 * @return the number of controls (always 2)
	 */
	@Override
	public int getNumberOfControls() {
		return 2; // Label + Composite(Text + Button)
	}

	/**
	 * Fills the field editor's controls into the given parent composite.
	 *
	 * This implementation creates a label in the first column and a composite
	 * containing both the text field and show/hide button in the remaining columns.
	 *
	 * @param parent the composite used as a parent for the field editor's controls
	 * @param numColumns the number of columns that the field editor should span
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		// Column 1: Label
		Label label = getLabelControl(parent);
		GridData labelData = new GridData();
		labelData.horizontalSpan = 1; // Label occupies the first column
		label.setLayoutData(labelData);

		// Column 2: Composite containing Text and Button
		textButtonComposite = getTextAndButtonComposite(parent);
		GridData compositeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		compositeData.horizontalSpan = numColumns - 1; // Composite occupies the remaining columns
		textButtonComposite.setLayoutData(compositeData);
	}

	/**
	 * Returns the text control for this field editor.
	 *
	 * This implementation ensures the text control is created within the internal composite
	 * rather than directly in the provided parent. This is necessary to maintain the
	 * custom layout with the show/hide button.
	 *
	 * @param parent the parent composite (used only to initialize the internal composite if needed)
	 * @return the text control
	 */
	@Override
	public Text getTextControl(Composite parent) {
		// Ensure the composite and its contents are created if they haven't been.
		// The actual parent used for the Text control is textButtonComposite.
		getTextAndButtonComposite(parent);
		// Return the cached text field, which is guaranteed to exist now.
		return super.getTextControl();
	}

	/**
	 * Sets the tooltip text for all controls in this field editor.
	 *
	 * This sets the tooltip on the label, text field, and provides a specific
	 * tooltip for the show/hide button.
	 *
	 * @param text the tooltip text to set
	 */
	public void setToolTipText(String text) {
		getLabelControl().setToolTipText(text);
		// Ensure text control exists before setting tooltip
		if (getTextControl() != null) {
			getTextControl().setToolTipText(text);
		}
		if (showPasswordButton != null) {
			showPasswordButton.setToolTipText("Show or hide password");
		}
	}

	/**
	 * Adjusts the field editor's controls to accommodate the specified number of columns.
	 *
	 * This implementation ensures the label takes one column and the text/button composite
	 * takes all remaining columns.
	 *
	 * @param numColumns the number of columns to adjust for
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		GridData labelData = (GridData) getLabelControl().getLayoutData();
		labelData.horizontalSpan = 1;

		if (textButtonComposite != null) {
			GridData compositeData = (GridData) textButtonComposite.getLayoutData();
			compositeData.horizontalSpan = numColumns - 1;
		}
	}

	/**
	 * Loads the value from the preference store.
	 * Calls {@link #updateControlStates()} after loading to ensure UI consistency.
	 */
	@Override
	protected void doLoad() {
		super.doLoad();
		// Ensure state is correct after loading value
		updateControlStates();
	}

	/**
	 * Loads the default value from the preference store.
	 * Calls {@link #updateControlStates()} after loading to ensure UI consistency.
	 */
	@Override
	protected void doLoadDefault() {
		super.doLoadDefault();
		// Ensure state is correct after loading default value
		updateControlStates();
	}

	/**
	 * Creates and returns the composite containing the text field and the show/hide button.
	 *
	 * This method creates a two-column composite with the text field in the first column
	 * and the show/hide button in the second. The text field is initially configured with
	 * bullet characters for masking and is non-editable when the password is hidden.
	 * The show/hide button is disabled if the text field is empty. The field starts
	 * shown if initially empty.
	 *
	 * @param parent the parent for the new composite
	 * @return the composite containing the text field and button
	 */
	private Composite getTextAndButtonComposite(Composite parent) {
		if (textButtonComposite == null) {
			textButtonComposite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, false); // 2 columns inside this composite
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.horizontalSpacing = 5;
			textButtonComposite.setLayout(layout);

			// Create Text field *inside* the composite
			final Text textField = super.getTextControl(textButtonComposite);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// Create Button *inside* the composite
			showPasswordButton = new Button(textButtonComposite, SWT.PUSH);
			showPasswordButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

			// Listener for text changes to update button enablement and visibility state
			textField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					updateControlStates();
				}
			});

			// Listener for button clicks to toggle visibility
			showPasswordButton.addSelectionListener(new SelectionAdapter() {
				/**
				 * Toggles password visibility when the show/hide button is clicked,
				 * only if the button is enabled (text field is not empty).
				 *
				 * @param e the selection event (not used)
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					// Only toggle if the button is enabled (i.e., text is not empty)
					if (showPasswordButton.isEnabled()) {
						showPassword = !showPassword;
						updateControlStates();
					}
				}
			});

			// Set initial state after controls and listeners are created
			updateControlStates();
		}
		return textButtonComposite;
	}

	/**
	 * Updates the state of the text field (echo char, editable, color) and the
	 * show/hide button (text, enabled, foreground color) based on the current text content and
	 * the showPassword flag.
	 *
	 * The field is displayed as visible and is editable when empty or when the showPassword
	 * flag is true ('effectively visible'). When not empty and showPassword is false,
	 * the echo character is used, and the field becomes non-editable.
	 * The show/hide button is disabled when the field is empty.
	 */
	private void updateControlStates() {
		Text textField = getTextControl();
		if (textField == null || textField.isDisposed() || showPasswordButton == null || showPasswordButton.isDisposed()) {
			return;
		}

		boolean isEmpty = textField.getText().isEmpty();

		// Button is enabled only if text field is not empty
		showPasswordButton.setEnabled(!isEmpty);
		showPasswordButton.setForeground(showPasswordButton.getDisplay()
				.getSystemColor(!isEmpty ? SWT.COLOR_WIDGET_FOREGROUND : SWT.COLOR_WIDGET_DISABLED_FOREGROUND));

		// Button text reflects the underlying state flag (showPassword), not the effective visibility
		showPasswordButton.setText(showPassword ? "Hide" : "Show");

		// Determine the effective display state based on the flag and emptiness
		boolean isEffectivelyVisible = showPassword || isEmpty;
		textField.setEchoChar(isEffectivelyVisible ? '\0' : '•');
		textField.setEditable(isEffectivelyVisible); // Editable only when effectively visible
		textField.setForeground(textField.getDisplay().getSystemColor(
				isEffectivelyVisible ? SWT.COLOR_WIDGET_FOREGROUND : SWT.COLOR_WIDGET_DISABLED_FOREGROUND));
	}

}