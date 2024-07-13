package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eclipse.plugin.aiassistant.browser.BrowserScriptGenerator;
import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.utility.Eclipse;

/**
 * Manages the context menu for the chat conversation area within the AI Assistant plugin.
 * This class is responsible for creating and handling interactions within the context menu
 * that appears when the user right-clicks in the chat message area. It supports operations
 * like copying, pasting, and reviewing changes directly from the chat interface.
 */
public class ChatConversationMenu {

	public static final String COPY_TO_CLIPBOARD_NAME = "Copy";
	public static final String COPY_TO_CLIPBOARD_TOOLTIP = "Copy to the Clipboard";
	public static final String COPY_TO_CLIPBOARD_ICON = "CopyToClipboard.png";
	public static final String REPLACE_SELECTION_NAME = "Replace Selection";
	public static final String REPLACE_SELECTION_TOOLTIP = "Replace Editor Selection with Selected Text";
	public static final String REPLACE_SELECTION_ICON = "ReplaceSelection.png";
	public static final String REVIEW_CHANGES_NAME = "Review Changes";
	public static final String REVIEW_CHANGES_TOOLTIP = "Open the 'Review Changes' Dialog";
	public static final String REVIEW_CHANGES_ICON = "ReviewChanges.png";
	public static final String PASTE_MESSAGE_NAME = "Paste To Message";
	public static final String PASTE_MESSAGE_TOOLTIP = "Paste the Clipboard Contents as a Message";
	public static final String PASTE_MESSAGE_ICON = "Paste.png";
	public static final String PASTE_CONTEXT_NAME = "Paste As Context";
	public static final String PASTE_CONTEXT_TOOLTIP = "Paste the Clipboard Contents as Context";
	public static final String PASTE_CONTEXT_ICON = "Paste.png";

	private final MainPresenter mainPresenter;
	private final ChatConversationArea chatConversationArea;

	private final BrowserScriptGenerator browserScriptGenerator;

	private MenuItemData[] browserFunctionMenuItemsData = {
			new MenuItemData(COPY_TO_CLIPBOARD_NAME, COPY_TO_CLIPBOARD_ICON, COPY_TO_CLIPBOARD_TOOLTIP,
					e -> handleCopySelection()),
			new MenuItemData(REPLACE_SELECTION_NAME, REPLACE_SELECTION_ICON, REPLACE_SELECTION_TOOLTIP,
					e -> handleReplaceSelection()),
			new MenuItemData(REVIEW_CHANGES_NAME, REVIEW_CHANGES_ICON, REVIEW_CHANGES_TOOLTIP,
					e -> handleReviewChanges())
	};

	private MenuItemData[] pasteMenuItemsData = {
			new MenuItemData(PASTE_MESSAGE_NAME, PASTE_MESSAGE_ICON, PASTE_MESSAGE_TOOLTIP,
					e -> handlePasteMessage()),
			new MenuItemData(PASTE_CONTEXT_NAME, PASTE_CONTEXT_ICON, PASTE_CONTEXT_TOOLTIP,
					e -> handlePasteContext())
	};

	private List<Image> imagesToDispose = new ArrayList<>();

	private Menu menu;
	
	// Used to disable all the menu options from setEnabled() call.
	private boolean menuEnabled = true;

	/**
     * Constructs a new ChatConversationMenu.
	 *
     * @param mainPresenter The main presenter of the application, handling core functionality.
     * @param chatConversationArea The area of the UI where chat conversations are displayed.
	 */
	public ChatConversationMenu(MainPresenter mainPresenter, ChatConversationArea chatConversationArea) {
		this.mainPresenter = mainPresenter;
		this.chatConversationArea = chatConversationArea;
		browserScriptGenerator = new BrowserScriptGenerator();
		this.menu = createMenu(chatConversationArea.getBrowser());
	}

	/**
     * Enables or disables the context menu based on the specified flag.
	 *
	 * @param enabled True to enable the context menu, false to disable it.
	 */
	public void setEnabled(boolean enabled) {
	    if (menuEnabled != enabled) {
	        menuEnabled = enabled;
	        Eclipse.runOnUIThreadAsync(() -> { updateMenuItemsVisibility(); });
	    }
	}

	/**
	 * Creates and returns a context menu for the chat conversation area.
	 *
	 * @param browser The browser widget that displays the chat messages.
	 * @return The newly created menu.
	 */
	private Menu createMenu(Browser browser) {
		Menu menu = new Menu(browser);

		for (MenuItemData itemData : browserFunctionMenuItemsData) {
			addMenuItem(menu, itemData.text, itemData.iconPath, itemData.toolTipText, itemData.listener);
		}
		new MenuItem(menu, SWT.SEPARATOR);
		for (MenuItemData itemData : pasteMenuItemsData) {
			addMenuItem(menu, itemData.text, itemData.iconPath, itemData.toolTipText, itemData.listener);
		}

		menu.addDisposeListener(e -> disposeImages());
		menu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				updateMenuItemsVisibility();
			}
			public void menuHidden(MenuEvent e) {
			}
		});

		browser.setMenu(menu);

		return menu;
	}

	/**
     * Adds a menu item to the specified menu with the given properties.
	 *
	 * @param menu        The menu to which the item is added.
	 * @param text        The text displayed on the menu item.
	 * @param iconPath    Optional path for the menu item's icon.
	 * @param toolTipText Tooltip text for the menu item.
	 * @param listener    Listener to handle the selection event.
	 */
	private void addMenuItem(Menu menu, String text, String iconPath, String toolTipText, Listener listener) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(text);
		if (iconPath != null) {
			Image iconImage = Eclipse.loadIcon(iconPath);
			menuItem.setImage(iconImage);
			imagesToDispose.add(iconImage); // Save the image for later disposal
		}
		menuItem.setToolTipText(toolTipText);
		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Updates the visibility and enabled state of menu items based on the current context.
	 */
	private void updateMenuItemsVisibility() {
	    boolean showBrowserFunctions = false;
	    boolean showPasteOptions = false;
	    if (menuEnabled) {
	    	showBrowserFunctions = !copyBrowserSelectedText().isEmpty();
	    	showPasteOptions = !Eclipse.getClipboardContents().isEmpty();
	    }
	    for (MenuItem item : menu.getItems()) {
	        switch (item.getText()) {
	        case COPY_TO_CLIPBOARD_NAME:
	        case REPLACE_SELECTION_NAME:
	        case REVIEW_CHANGES_NAME:
	            item.setEnabled(showBrowserFunctions);
	            break;
	        case PASTE_MESSAGE_NAME:
	        case PASTE_CONTEXT_NAME:
	            item.setEnabled(!showBrowserFunctions && showPasteOptions);
	            break;
	        }
	    }
	}

	/**
	 * Handles the action of copying the selected text from the browser to the clipboard.
	 */
	private void handleCopySelection() {
	    chatConversationArea.handleCopySelection(copyBrowserSelectedText());
	}
	
	/**
	 * Handles the action of replacing the selected text in the editor with the text selected
	 * in the browser.
	 */
	private void handleReplaceSelection() {
	    chatConversationArea.handleReplaceSelection(copyBrowserSelectedText());
	}
	
	/**
	 * Handles the action of reviewing changes based on the selected text in the browser.
	 */
	private void handleReviewChanges() {
	    chatConversationArea.handleReviewChanges(copyBrowserSelectedText());
	}
	
	/**
     * Handles the action to paste a predefined message into the chat.
	 */
	private void handlePasteMessage() {
		mainPresenter.sendPredefinedPrompt(Prompts.PASTE_MESSAGE);
	}

	/**
     * Handles the action to paste a predefined context into the chat.
	 */
	private void handlePasteContext() {
		mainPresenter.sendPredefinedPrompt(Prompts.PASTE_CONTEXT);
	}

    /**
     * Copies the currently selected text from the browser.
     *
     * @return The selected text, or an empty string if no text is selected.
     */
	private String copyBrowserSelectedText() {
		Object result = Eclipse.evaluateScript(chatConversationArea.getBrowser(),
				browserScriptGenerator.generateGetSelectionScript());
        return result instanceof String ? (String) result : "";
	}

    /**
     * Disposes of all images that have been used in the menu items.
     */
	private void disposeImages() {
	    Eclipse.runOnUIThreadAsync(() -> {
	        for (Image image : imagesToDispose) {
	            if (!image.isDisposed()) {
	                image.dispose();
	            }
	        }
	        imagesToDispose.clear();
	    });
	}

    /**
     * Data structure to hold menu item properties and associated actions.
     */
	class MenuItemData {
		String text;
		String iconPath;
		String toolTipText;
		Listener listener;

		MenuItemData(String text, String iconPath, String toolTipText, Listener listener) {
			this.text = text;
			this.iconPath = iconPath;
			this.toolTipText = toolTipText;
			this.listener = listener;
		}
	}

}