package eclipse.plugin.aiassistant.preferences;

import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page for the prompt templates.
 */
public class PromptTemplatesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private PromptTemplatesPreferencePresenter preferencePresenter;
	
	private SashForm sashForm;
	private Composite mainContainer;
	private List list;
	private Text textArea;

	 /**
     * Constructs a new `PromptTemplatesPreferencePage`.
     */
	public PromptTemplatesPreferencePage() {
		setPreferenceStore(Preferences.getDefault());
		setDescription("View or edit the prompt templates.");
		preferencePresenter = new PromptTemplatesPreferencePresenter();
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
     * Creates the contents of the preference page.
     *
     * @param parent The parent composite for the contents.
     * @return The created contents.
     */
	@Override
	protected Control createContents(Composite parent) {
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		mainContainer = new Composite(sashForm, SWT.NONE);
		mainContainer.setLayout(new GridLayout(1, false));
		mainContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		list = new List(mainContainer, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		list.setToolTipText("Select Prompt Template");
		list.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Objects.requireNonNull(preferencePresenter);
				int selectedIndex = list.getSelectionIndex();
				preferencePresenter.setSelectedPrompt(selectedIndex);
			}
		});
		
		textArea = new Text(mainContainer, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		preferencePresenter.registerView(this);

		return sashForm;
	}
	
	  /**
     * Contributes buttons to the preference page.
     *
     * @param parent The parent composite for the buttons.
     */
	@Override
	protected void contributeButtons(Composite parent) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText("&Restore All Defaults");
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		button.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		    	preferencePresenter.resetAllPrompts();
		    }
		});
	}
	
	 /**
     * Performs the default action when the "OK" button is pressed.
     * NOTE: performApply() doesn't work with "Apply and Close" so use performOk() instead.
     *
     * @return Whether the default action was performed successfully.
     */
	@Override
	public boolean performOk() {
		if (list != null && !list.isDisposed()) {
			// Save the current prompt text to the preference store
			int selectedIndex = list.getSelectionIndex();
			if (selectedIndex != -1 && textArea != null && !textArea.isDisposed()) {
				preferencePresenter.savePrompt(selectedIndex, textArea.getText());
			}
		}
		return super.performOk();
	}

	/**
     * Resets the preference page to its default values.
     */
	@Override
	protected void performDefaults() {
		int selectedIndex = list.getSelectionIndex();
		if (selectedIndex != -1) {
			preferencePresenter.resetPrompt(selectedIndex);
		}
		super.performDefaults();
	}
	
	/**
     * Deselects all items in the list.
     */
	public void deselectAll() {
		list.deselectAll();
	}
	
	/**
     * Sets the prompt templates in the list.
     *
     * @param prompts The prompt templates to set.
     */
	public void setPrompts(String[] prompts) {
		list.setItems(prompts);
	}

	/**
     * Sets the current prompt template in the text area.
     *
     * @param selectedItem The prompt template to set.
     */
	public void setCurrentPrompt(String selectedItem) {
		textArea.setText(selectedItem);
	}
	
}