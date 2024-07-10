package eclipse.plugin.aiassistant.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eclipse.plugin.aiassistant.browser.BrowserScriptGenerator;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * Manages the context menu for the chat conversation area within the AI
 * Assistant plugin. This class is responsible for creating and handling
 * interactions within the context menu that appears when the user right-clicks
 * in the chat message area.
 */
public class ChatConversationMenu {

	public static final String COPY_NAME = "Copy";
	public static final String COPY_TOOLTIP = "Copy selected text";

	public static final String FORWARD_NAME = "Forward";
	public static final String FORWARD_TOOLTIP = "Navigate to the next session history item";
	public static final String BACK_NAME = "Back";
	public static final String BACK_TOOLTIP = "Navigate to the previous session history item";

	public static final String PASTE_MESSAGE_NAME = "Paste To Message";
	public static final String PASTE_MESSAGE_TOOLTIP = "Paste the Clipboard Contents as a Message";
	public static final String PASTE_CONTEXT_NAME = "Paste As Context";
	public static final String PASTE_CONTEXT_TOOLTIP = "Paste the Clipboard Contents as Context";
	public static final String PASTE_ICON = "Paste.png";

	private final MainPresenter mainPresenter;

	private final BrowserScriptGenerator browserScriptGenerator;

	private Menu menu;

	/**
	 * Initializes a new instance of ChatConversationMenu with the specified main
	 * presenter and browser.
	 *
	 * @param mainPresenter The main presenter of the application, handling core
	 *                      functionality.
	 * @param browser       The browser widget used to display and interact with
	 *                      chat messages.
	 */
	public ChatConversationMenu(MainPresenter mainPresenter, Browser browser) {
		this.mainPresenter = mainPresenter;
		this.browserScriptGenerator = new BrowserScriptGenerator();
		this.menu = createMenu(browser);
	}

	/**
	 * Enables or disables the context menu.
	 *
	 * @param enabled True to enable the context menu, false to disable it.
	 */
	public void setEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			menu.setEnabled(enabled); // ### BUGGY... ###
			// menu.setVisible(b); // ### BROKEN... ###
		});
	}

	/**
	 * Creates and returns a context menu for the chat conversation area.
	 *
	 * @param browser The browser widget that displays the chat messages.
	 * @return The newly created menu.
	 */
	private Menu createMenu(Browser browser) {
		Menu menu = new Menu(browser);
		addMenuItem(menu, FORWARD_NAME, null, FORWARD_TOOLTIP, e -> {
		    Eclipse.executeScript(browser, browserScriptGenerator.generateNavigateForwardScript());
		});
		addMenuItem(menu, BACK_NAME, null, BACK_TOOLTIP, e -> {
		    Eclipse.executeScript(browser, browserScriptGenerator.generateNavigateBackScript());
		});
		new MenuItem(menu, SWT.SEPARATOR);
		addMenuItem(menu, COPY_NAME, null, COPY_TOOLTIP, e -> copySelectedText(browser));
		Image pasteIcon = Eclipse.loadIcon(PASTE_ICON);
		new MenuItem(menu, SWT.SEPARATOR);
		addMenuItem(menu, PASTE_MESSAGE_NAME, pasteIcon, PASTE_MESSAGE_TOOLTIP, this::handlePasteMessage);
		addMenuItem(menu, PASTE_CONTEXT_NAME, pasteIcon, PASTE_CONTEXT_TOOLTIP, this::handlePasteContext);
		browser.setMenu(menu);
		pasteIcon.dispose();
		return menu;
	}

	/**
	 * Adds a menu item to the specified menu.
	 *
	 * @param menu        The menu to which the item is added.
	 * @param text        The text displayed on the menu item.
	 * @param iconImage   Optional icon for the menu item.
	 * @param toolTipText Tooltip text for the menu item.
	 * @param listener    Listener to handle the selection event.
	 */
	private void addMenuItem(Menu menu, String text, Image iconImage, String toolTipText, Listener listener) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(text);
		if (iconImage != null) {
			menuItem.setImage(iconImage);
		}
		menuItem.setToolTipText(toolTipText);
		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Handles the action to paste a predefined message into the chat.
	 * This method triggers the main presenter to send a specific prompt defined for pasting messages.
	 *
	 * @param e The event triggered by selecting the paste message menu item.
	 */
	private void handlePasteMessage(Event e) {
	    mainPresenter.sendPredefinedPrompt(Prompts.PASTE_MESSAGE);
	}
	
	/**
	 * Handles the action to paste a predefined context into the chat.
	 * This method triggers the main presenter to send a specific prompt defined for pasting context.
	 *
	 * @param e The event triggered by selecting the paste context menu item.
	 */
	private void handlePasteContext(Event e) {
	    mainPresenter.sendPredefinedPrompt(Prompts.PASTE_CONTEXT);
	}
	
	/**
	 * Copies the currently selected text in the browser to the system clipboard.
	 * This method executes a JavaScript script in the browser to retrieve the selected text,
	 * and if text is selected, it sets this text to the system clipboard.
	 *
	 * @param browser The browser from which the selected text is to be copied.
	 */
	private void copySelectedText(Browser browser) {
		var result =  Eclipse.evaluateScript(browser, browserScriptGenerator.generateGetSelectionScript());
	    if (result instanceof String) {
	        Eclipse.setClipboardContents((String) result);
	    }
	}
	
}