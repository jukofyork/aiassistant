package eclipse.plugin.aiassistant.preferences;

import java.util.Arrays;

import eclipse.plugin.aiassistant.prompt.Prompts;

/**
 * This class represents a presenter for the prompt templates preference page.
 */
public class PromptTemplatesPreferencePresenter {
	
	private PromptTemplatesPreferencePage view;

	/**
     * Constructs a new `PromptTemplatesPreferencePresenter`.
     */
	public PromptTemplatesPreferencePresenter() {
	}

	/**
     * Registers the given view with this presenter.
     *
     * @param view The view to register.
     */
	public void registerView(PromptTemplatesPreferencePage view) {
		this.view = view;
		initializeView();
	}
	
	/**
     * Sets the selected prompt in the view.
     *
     * @param index The index of the selected prompt.
     */
	public void setSelectedPrompt(int index) {
		if (index < 0) {
			view.setCurrentPrompt("");
		} else {
			var prompt = Preferences.getDefault().getString(getPreferenceName(index));
			view.setCurrentPrompt(prompt);
		}
	}

	/**
     * Saves the given text as the prompt for the given index.
     *
     * @param selectedIndex The index to save the prompt for.
     * @param text The text to save as the prompt.
     */
	public void savePrompt(int selectedIndex, String text) {
		Preferences.getDefault().setValue(getPreferenceName(selectedIndex), text);
	}

	/**
     * Resets the prompt for the given index to its default value.
     *
     * @param selectedIndex The index of the prompt to reset.
     */
	public void resetPrompt(int selectedIndex) {
		var propertyName = getPreferenceName(selectedIndex);
		var defaultValue = Preferences.getDefault().getDefaultString(propertyName);
		Preferences.getDefault().setValue(propertyName, defaultValue);
		view.setCurrentPrompt(defaultValue);
	}

	/**
     * Resets all prompts to their default values.
     */
	public void resetAllPrompts() {
		for (int i = 0; i < Prompts.values().length; i++) {
			resetPrompt(i);
		}
		view.setCurrentPrompt("");
		view.deselectAll();
	}

	private void initializeView() {
		String[] prompts = Arrays.stream(Prompts.values()).map(Prompts::getTaskName).toArray(String[]::new);
		view.setPrompts(prompts);
	}

	private String getPreferenceName(int index) {
		return Prompts.values()[index].preferenceName();
	}
	
}