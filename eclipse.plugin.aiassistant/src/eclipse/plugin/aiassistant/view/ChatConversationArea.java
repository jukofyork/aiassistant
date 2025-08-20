package eclipse.plugin.aiassistant.view;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import eclipse.plugin.aiassistant.browser.ApplyPatchBrowserFunction;
import eclipse.plugin.aiassistant.browser.BrowserScriptGenerator;
import eclipse.plugin.aiassistant.browser.CopyCodeBrowserFunction;
import eclipse.plugin.aiassistant.browser.DisableableBrowserFunction;
import eclipse.plugin.aiassistant.browser.ReplaceSelectionBrowserFunction;
import eclipse.plugin.aiassistant.browser.ReviewChangesBrowserFunction;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.chat.ChatRole;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.MarkdownToHtmlConverter;

/**
 * The ChatConversationArea class represents the chat area in the AssistAI
 * plugin for Eclipse. It handles displaying messages, scrolling, and
 * interacting with browser functions.
 */
public class ChatConversationArea {

	public static final String BROWSER_TOOLTIP = """
			Right Click: Show Context Menu
			Ctrl+Scrollwheel: Navigate Top/Bottom
			Shift+Scrollwheel: Navigate Messages""";

	public static final String COPY_CODE_FUNCTION_NAME = "eclipseCopyCode";
	public static final String REPLACE_SELECTION_FUNCTION_NAME = "eclipseReplaceSelection";
	public static final String REVIEW_CHANGES_FUNCTION_NAME = "eclipseReviewChanges";
	public static final String APPLY_PATCH_FUNCTION_NAME = "eclipseApplyPatch";

	private final MainPresenter mainPresenter;

	private final Browser browser;
	private final ChatConversationMenu browserMenu;
	private final BrowserScriptGenerator browserScriptGenerator;
	private List<DisableableBrowserFunction> browserFunctions;

	// Smooth scrolling is too janky whilst streaming output getting written.
	private boolean useSmoothScroll = true;

	private boolean useAsyncExecution = true;

	/**
	 * Constructs a new ChatConversationArea instance.
	 *
	 * @param mainPresenter The MainPresenter instance.
	 * @param parent        The Composite parent widget.
	 */
	public ChatConversationArea(MainPresenter mainPresenter, Composite parent) {
		this.mainPresenter = mainPresenter;
		browser = new Browser(parent, SWT.EDGE);
		browser.setLayoutData(Eclipse.createGridData(true, true));
		configureTextToolTip(BROWSER_TOOLTIP);
		addMouseWheelListener();
		addShiftKeyListener();
		browserMenu = new ChatConversationMenu(mainPresenter, this);
		browserScriptGenerator = new BrowserScriptGenerator();
		initialize();
		setBrowserFunctions();
	}

	/**
	 * Initializes the chat conversation area.
	 */
	public void initialize() {
		setText(browserScriptGenerator.generateInitialHtml());
		executeScript("");
	}

	/**
	 * Checks if the browser is disposed.
	 *
	 * @return True if the browser is disposed, false otherwise.
	 */
	public boolean isDisposed() {
		return (browser == null || browser.isDisposed());
	}

	/**
	 * Sets the enabled state of the chat conversation area and its functions.
	 *
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		setAllBrowserFunctionsEnabled(enabled);
		browserMenu.setEnabled(enabled);
		useSmoothScroll = enabled;
	}

	/**
	 * Sets whether script execution should be performed asynchronously.
	 *
	 * @param enabled True to use asynchronous execution (default), false to use synchronous execution.
	 */
	public void setAsyncExecution(boolean enabled) {
		useAsyncExecution = enabled;
	}

	/**
	 * Sets the visible state of the chat conversation area.
	 *
	 * @param enabled True to enable, false to disable.
	 */
	public void setVisible(boolean enabled) {
		browser.setVisible(enabled);
	}

	/**
	 * Adds a new message to the chat conversation area.
	 *
	 * @param message The ChatMessage instance to add.
	 */
	public void newMessage(ChatMessage message) {
		String script = browserScriptGenerator.generateNewMessageElementScript(message);
		executeScript(script);
		updateMessage(message);
	}

	/**
	 * Updates an existing message in the chat conversation area.
	 *
	 * @param message The ChatMessage instance to update.
	 */
	public void updateMessage(ChatMessage message) {
		String html = MarkdownToHtmlConverter.convertMarkdownToHtml(message.getContent(), message.getRole() == ChatRole.ASSISTANT);
		String script = browserScriptGenerator.generateUpdateMessageScript(html, message.getId());
		executeScript(script);
	}

	/**
	 * Removes a message from the chat conversation area.
	 *
	 * @param messageId The UUID of the message to remove.
	 */
	public void removeMessage(UUID messageId) {
		executeScript(browserScriptGenerator.generateRemoveMessageScript(messageId));
	}

	/**
	 * Scrolls the chat conversation area to the top.
	 */
	public void scrollToTop() {
		removeAllSelectionBorders();
		executeScript(browserScriptGenerator.generateScrollToTopScript(useSmoothScroll));
	}

	/**
	 * Scrolls the chat conversation area to the bottom.
	 */
	public void scrollToBottom() {
		removeAllSelectionBorders();
		executeScript(browserScriptGenerator.generateScrollToBottomScript(useSmoothScroll));
	}

	/**
	 * Scrolls the chat conversation area to a specific message and highlights it.
	 *
	 * @param messageId The UUID of the message to scroll to.
	 */
	public void scrollToMessage(UUID messageId) {
		removeAllSelectionBorders();
		setSelectionBorder(messageId);
		executeScript(browserScriptGenerator.generateScrollToMessageScript(messageId, useSmoothScroll));
	}

	/**
	 * Checks if the chat conversation area's scrollbar is at the bottom by
	 * evaluating a script in the browser.
	 *
	 * @return True if the scrollbar is at the bottom, false otherwise.
	 */
	public boolean isScrollbarAtBottom() {
		return (boolean) Eclipse.evaluateScript(browser, browserScriptGenerator.generateIsScrollbarAtBottomScript());
	}

	/**
	 * Sets a border around a specific message in the chat conversation area to indicate selection.
	 *
	 * @param messageId The UUID of the message to set the border for.
	 */
	public void setSelectionBorder(UUID messageId) {
		executeScript(browserScriptGenerator.generateSetBorderScript(messageId));
	}

	/**
	 * Removes all borders around messages in the chat conversation area.
	 */
	public void removeAllSelectionBorders() {
		executeScript(browserScriptGenerator.generateRemoveAllBordersScript());
	}

	/**
	 * Gets the Browser widget instance used for displaying the chat conversation.
	 *
	 * @return The Browser widget instance.
	 */
	public Browser getBrowser() {
		return browser;
	}

	/**
	 * Handles copying the selected text to the clipboard.
	 * Uses the copy code browser function to perform the copy operation.
	 *
	 * @param selectedText The text that was selected in the browser.
	 */
	public void handleCopySelection(String selectedText) {
		if (!selectedText.isEmpty()) {
			browserFunctions.get(0).function(new Object[]{selectedText});
		}
	}

	/**
	 * Handles replacing the current selection in the active editor with the selected text.
	 * Uses the replace selection browser function to perform the replacement.
	 *
	 * @param selectedText The text that was selected in the browser to replace with.
	 */
	public void handleReplaceSelection(String selectedText) {
		if (!selectedText.isEmpty()) {
			browserFunctions.get(1).function(new Object[]{selectedText});
		}
	}

	/**
	 * Handles opening a review dialog for the selected text changes.
	 * Uses the review changes browser function to show the review interface.
	 *
	 * @param selectedText The text that was selected in the browser to review.
	 */
	public void handleReviewChanges(String selectedText) {
		if (!selectedText.isEmpty()) {
			browserFunctions.get(2).function(new Object[]{selectedText});
		}
	}

	/**
	 * Sets the text content of the browser widget asynchronously on the UI thread.
	 *
	 * @param html The HTML content to set.
	 */
	private void setText(String html) {
		Eclipse.runOnUIThreadAsync(() -> browser.setText(html));
	}

	/**
	 * Configures the tooltip text for the browser widget.
	 *
	 * @param tooltipText The tooltip text to set.
	 */
	private void configureTextToolTip(String tooltipText) {
		if (!Preferences.disableTooltips()) {
			browser.setToolTipText(tooltipText);
		}
	}

	/**
	 * Adds a mouse wheel listener to the browser widget for handling scrolling and
	 * navigation.
	 */
	private void addMouseWheelListener() {
		browser.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int wheelDelta = e.count;
				if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
					handleControlAndScrolled(wheelDelta);
				} else if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD2) {
					handleShiftAndScrolled(wheelDelta);
				}
			}
		});
	}

	/**
	 * Adds a key listener to the display for handling Shift key events.
	 */
	private void addShiftKeyListener() {
		Display display = browser.getDisplay();
		display.addFilter(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.SHIFT) {
					handleShiftKeyPressed();
				}
			}
		});
		display.addFilter(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.SHIFT) {
					handleShiftKeyReleased();
				}
			}
		});
	}

	/**
	 * Handles Control + Scrollwheel events by scrolling to the top or bottom of the
	 * chat conversation area.
	 *
	 * @param wheelDelta The scroll wheel delta value.
	 */
	private void handleControlAndScrolled(int wheelDelta) {
		if (wheelDelta > 0) {
			mainPresenter.onScrollToTop();
		} else if (wheelDelta < 0) {
			mainPresenter.onScrollToBottom();
		}
	}

	/**
	 * Handles Shift + Scrollwheel events by moving through the messages in the chat
	 * conversation area.
	 *
	 * @param wheelDelta The scroll wheel delta value.
	 */
	private void handleShiftAndScrolled(int wheelDelta) {
		if (wheelDelta > 0) {
			mainPresenter.onScrollUp();
		} else if (wheelDelta < 0) {
			mainPresenter.onScrollDown();
		}
	}

	/**
	 * Handles the Shift key pressed event by calling the corresponding method in the main presenter.
	 */
	private void handleShiftKeyPressed() {
		mainPresenter.onShiftKeyPressed();
	}

	/**
	 * Handles the Shift key released event by calling the corresponding method in the main presenter.
	 */
	private void handleShiftKeyReleased() {
		mainPresenter.onShiftKeyReleased();
	}

	/**
	 * Initializes browser functions for handling various actions in the chat
	 * conversation area.
	 */
	private void setBrowserFunctions() {
		browserFunctions = new ArrayList<>();
		browserFunctions.add(new CopyCodeBrowserFunction(browser, COPY_CODE_FUNCTION_NAME));
		browserFunctions.add(new ReplaceSelectionBrowserFunction(browser, REPLACE_SELECTION_FUNCTION_NAME));
		browserFunctions.add(new ReviewChangesBrowserFunction(browser, REVIEW_CHANGES_FUNCTION_NAME));
		browserFunctions.add(new ApplyPatchBrowserFunction(browser, APPLY_PATCH_FUNCTION_NAME));
	}

	/**
	 * Sets the enabled state of all browser functions.
	 *
	 * @param enabled True to enable, false to disable.
	 */
	private void setAllBrowserFunctionsEnabled(boolean enabled) {
		for (DisableableBrowserFunction function : browserFunctions) {
			function.setEnabled(enabled);
		}
	}

	/**
	 * Executes a JavaScript script in the browser widget.
	 * The execution mode depends on the useAsyncExecution flag.
	 *
	 * @param script The JavaScript code to execute in the browser.
	 */
	private void executeScript(String script) {
		if (useAsyncExecution) {
			Eclipse.executeScript(browser,script);
		} else {
			Eclipse.evaluateScript(browser, script);
		}
	}

}