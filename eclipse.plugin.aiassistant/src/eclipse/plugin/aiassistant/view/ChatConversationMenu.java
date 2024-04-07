package eclipse.plugin.aiassistant.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * This class represents the chat message area menu in the application. It is
 * responsible for creating and managing the context menu that appears when the
 * user right-clicks on the chat message area.
 */
public class ChatConversationMenu {
	
	public static final String PASTE_MESSAGE_NAME = "Paste To Message";
	public static final String PASTE_MESSAGE_TOOLTIP = "Paste the Clipboard Contents as a Message";
	public static final String PASTE_CONTEXT_NAME = "Paste As Context";
	public static final String PASTE_CONTEXT_TOOLTIP = "Paste the Clipboard Contents as Context";
	public static final String PASTE_ICON = "Paste.png";

	private final MainPresenter mainPresenter;

	private Menu menu;

	/**
	 * Constructs a new instance of the ChatMessageAreaMenu class with the specified
	 * main presenter and browser.
	 *
	 * @param mainPresenter The main presenter of the application.
	 * @param browser       The browser widget that displays the chat messages.
	 */
	public ChatConversationMenu(MainPresenter mainPresenter, Browser browser) {
		this.mainPresenter = mainPresenter;
		menu = createMenu(browser);
	}

	/**
	 * Enables or disables the context menu for the chat message area based on the
	 * specified boolean value.
	 *
	 * @param b True to enable the context menu, false to disable it.
	 */
	public void setEnabled(boolean enabled) {
		Eclipse.runOnUIThreadAsync(() -> {
			menu.setEnabled(enabled); // ### BUGGY... ###
			// menu.setVisible(b); // ### BROKEN... ###
		});
	}

	/**
	 * Creates a context menu for the chat conversation area in the application.
	 *
	 * @param browser The browser widget that displays the chat messages.
	 * @return The created menu instance.
	 */
	private Menu createMenu(Browser browser) {
		Menu menu = new Menu(browser);
		Image pasteIcon = Eclipse.loadIcon(PASTE_ICON);
		addMenuItem(menu, PASTE_MESSAGE_NAME, pasteIcon, PASTE_MESSAGE_TOOLTIP,
				Prompts.PASTE_MESSAGE);
		addMenuItem(menu, PASTE_CONTEXT_NAME, pasteIcon, PASTE_CONTEXT_TOOLTIP,
				Prompts.PASTE_CONTEXT);
		browser.setMenu(menu);
		pasteIcon.dispose();
		return menu;
	}

	/**
	 * Adds a new menu item to the specified menu with the given parameters.
	 *
	 * @param menu        The menu to which the new item will be added.
	 * @param text        The text displayed for the menu item.
	 * @param iconImage   The image displayed for the menu item.
	 * @param toolTipText The tooltip text displayed when hovering over the menu
	 *                    item.
	 * @param prompt      The predefined prompt associated with the menu item.
	 */
	private void addMenuItem(Menu menu, String text, Image iconImage, String toolTipText, Prompts prompt) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(text);
		menuItem.setImage(iconImage);
		menuItem.setToolTipText(toolTipText);
		addMenuItemSelectionListener(menuItem, prompt);
	}

	/**
	 * Adds a selection listener to the specified menu item that handles the user's
	 * selection of the item.
	 *
	 * @param menuItem The menu item to which to add the selection listener.
	 * @param prompt   The predefined prompt associated with the menu item.
	 */
	private void addMenuItemSelectionListener(MenuItem menuItem, Prompts prompt) {
		menuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleMenuItemSelection(prompt);
			}
		});
	}

	/**
	 * Handles the user's selection of a menu item by sending the associated
	 * predefined prompt.
	 *
	 * @param prompt The predefined prompt associated with the selected menu item.
	 */
	private void handleMenuItemSelection(Prompts prompt) {
		mainPresenter.sendPredefinedPrompt(prompt);
	}

}