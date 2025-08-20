package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * Manages the tab button bar with previous, next, new tab, refresh, import, export, and close all buttons.
 */
public class TabButtonBarArea {

	public static final String PREVIOUS_NAME = "";
	public static final String PREVIOUS_TOOLTIP = "Previous Tab";
	public static final String PREVIOUS_ICON = "ArrowLeft.png";
	public static final String NEXT_NAME = "";
	public static final String NEXT_TOOLTIP = "Next Tab";
	public static final String NEXT_ICON = "ArrowRight.png";
	public static final String NEW_TAB_NAME = "";
	public static final String NEW_TAB_TOOLTIP = "New Tab";
	public static final String NEW_TAB_ICON = "NewTab.png";

	private final MainPresenter mainPresenter;

	private final List<ButtonConfig> buttonConfigs = List.of(
			new ButtonConfig(PREVIOUS_NAME, PREVIOUS_TOOLTIP, PREVIOUS_ICON, this::onPrevious),
			new ButtonConfig(NEXT_NAME, NEXT_TOOLTIP, NEXT_ICON, this::onNext),
			new ButtonConfig(NEW_TAB_NAME, NEW_TAB_TOOLTIP, NEW_TAB_ICON, this::onNewTab));

	private Composite buttonContainer;
	private List<Button> buttons;

	/**
	 * Constructs a new TabButtonBarArea instance with the given main presenter and
	 * parent composite.
	 *
	 * @param mainPresenter The main presenter for the application.
	 * @param parent        The parent composite for the button container.
	 */
	public TabButtonBarArea(MainPresenter mainPresenter, Composite parent) {
		this.mainPresenter = mainPresenter;
		buttonContainer = createButtonContainer(parent);
		createButtons();
	}

	/**
	 * Gets the button container composite.
	 *
	 * @return The button container composite.
	 */
	public Composite getButtonContainer() {
		return buttonContainer;
	}

	/**
	 * Sets the input enabled state for all buttons in the tab button bar area.
	 *
	 * @param enabled True to enable input, false to disable it.
	 */
	public void setInputEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			for (int i = 0; i < buttons.size(); i++) {
				Button button = buttons.get(i);
				button.setEnabled(enabled);
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
				if (button.getToolTipText().equals(PREVIOUS_TOOLTIP)) {
					button.setEnabled(mainPresenter.getTabCount() > 1);
				} else if (button.getToolTipText().equals(NEXT_TOOLTIP)) {
					button.setEnabled(mainPresenter.getTabCount() > 1);
				}
			}
		});
	}

	/**
	 * Creates a new Composite to contain buttons in the tab button bar area.
	 *
	 * @param parent The parent composite for the button container.
	 * @return The newly created button container composite.
	 */
	private Composite createButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		// 7 buttons + 2 separators = 9 columns
		GridLayout layout = new GridLayout(9, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 2;
		container.setLayout(layout);
		return container;
	}

	/**
	 * Creates and initializes buttons based on the provided button configurations.
	 */
	private void createButtons() {
		buttons = new ArrayList<>();
		for (int i = 0; i < buttonConfigs.size(); i++) {
			ButtonConfig config = buttonConfigs.get(i);

			// Add separator after previous/next buttons (index 1)
			if (i == 2) {
				createSeparator();
			}
			// Add separator after new tab/refresh buttons (index 3)
			if (i == 4) {
				createSeparator();
			}

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
	 * Creates a vertical separator in the button container.
	 */
	private void createSeparator() {
		Label separator = new Label(buttonContainer, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData.widthHint = 1;
		gridData.heightHint = 16; // Fixed height to match typical button height
		separator.setLayoutData(gridData);
	}

	/**
	 * Handles the 'Previous' button click event by delegating to the main presenter.
	 */
	private void onPrevious() {
		mainPresenter.onPreviousTab();
	}

	/**
	 * Handles the 'Next' button click event by delegating to the main presenter.
	 */
	private void onNext() {
		mainPresenter.onNextTab();
	}

	/**
	 * Handles the 'New Tab' button click event by delegating to the main presenter.
	 */
	private void onNewTab() {
		mainPresenter.onNewTab();
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