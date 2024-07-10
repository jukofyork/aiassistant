package eclipse.plugin.aiassistant.browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.core.runtime.FileLocator;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.chat.ChatRole;
import eclipse.plugin.aiassistant.preferences.Preferences;

/**
 * The BrowserScriptGenerator class generates HTML, CSS, and JavaScript code
 * snippets for the chat interface.
 */
public class BrowserScriptGenerator {

	private String css;
	private String js;

	/**
	 * Constructs a new BrowserScriptGenerator instance and loads the required CSS
	 * and JavaScript files.
	 */
	public BrowserScriptGenerator() {
		css = loadCssFiles(Constants.CSS_FILENAMES);
		js = loadJsFiles(Constants.JS_FILENAMES);
	}

	/**
	 * Generates the initial HTML structure for the chat interface.
	 *
	 * @return The initial HTML structure as a string.
	 */
	public String generateInitialHtml() {
		// TODO: Find a better way to substitute font sizes dynamically.
		String newCss = substituteFontSizes(css);
		return "<html><style>" + newCss + "</style><script>" + js
				+ "</script><body><div id=\"content\"></div></body></html>";
	}

	/**
	 * Generates a JavaScript code snippet to create a new chat message element.
	 *
	 * @param message The ChatMessage object containing the message data.
	 * @return A JavaScript code snippet that creates a new chat message element.
	 */
	public String generateNewMessageElementScript(ChatMessage message) {
		return "node = document.createElement(\"div\");\n" + "node.setAttribute(\"id\", \"message-"
				+ message.getId().toString() + "\");\n" + "node.setAttribute(\"class\", \""
				+ getCssClassForRole(message) + "\");\n" + "document.getElementById(\"content\").appendChild(node);";
	}

	/**
	 * Generates a JavaScript code snippet to update the content of an existing chat
	 * message element.
	 *
	 * @param html      The HTML content to set for the chat message element.
	 * @param messageId The unique identifier of the chat message element.
	 * @return A JavaScript code snippet that updates the content of an existing
	 *         chat message element.
	 */
	public String generateUpdateMessageScript(String html, UUID messageId) {
		return "document.getElementById(\"message-" + messageId.toString() + "\").innerHTML = '" + html
				+ "';hljs.highlightAll();";
	}

	/**
	 * Generates a JavaScript code snippet to remove a chat message element from the
	 * DOM.
	 *
	 * @param messageId The unique identifier of the chat message element to remove.
	 * @return A JavaScript code snippet that removes a chat message element from
	 *         the DOM.
	 */
	public String generateRemoveMessageScript(UUID messageId) {
		return "var element = document.getElementById('message-" + messageId.toString() + "');"
				+ "element.parentNode.removeChild(element);";
	}

	/**
	 * Generates a JavaScript code snippet to scroll the window to the top.
	 *
	 * @param smoothScroll A boolean indicating whether smooth scrolling should be
	 *                     used.
	 * @return A JavaScript code snippet that scrolls the window to the top.
	 */
	public String generateScrollToTopScript(boolean smoothScroll) {
		return "window.scroll({top: 0, left: 0, behavior: '" + (smoothScroll ? "smooth" : "auto") + "' });";
	}

	/**
	 * Generates a JavaScript code snippet to scroll the window to the bottom.
	 *
	 * @param smoothScroll A boolean indicating whether smooth scrolling should be
	 *                     used.
	 * @return A JavaScript code snippet that scrolls the window to the bottom.
	 */
	public String generateScrollToBottomScript(boolean smoothScroll) {
		return "window.scroll({top: document.body.scrollHeight, left: 0, behavior: '"
				+ (smoothScroll ? "smooth" : "auto") + "' });";
	}

	/**
	 * Generates a JavaScript code snippet to scroll the window to a specific chat
	 * message element.
	 *
	 * @param messageId    The unique identifier of the chat message element to
	 *                     scroll to.
	 * @param smoothScroll A boolean indicating whether smooth scrolling should be
	 *                     used.
	 * @return A JavaScript code snippet that scrolls the window to a specific chat
	 *         message element.
	 */
	public String generateScrollToMessageScript(UUID messageId, boolean smoothScroll) {
		return "document.getElementById(\"message-" + messageId.toString() + "\").scrollIntoView({behavior: '"
				+ (smoothScroll ? "smooth" : "auto") + "'});";
	}

	/**
	 * Generates a JavaScript code snippet to check if the scrollbar is at the
	 * bottom of the window.
	 *
	 * @return A JavaScript code snippet that checks if the scrollbar is at the
	 *         bottom of the window.
	 */
	public String generateIsScrollbarAtBottomScript() {
		// https://stackoverflow.com/questions/9439725/how-to-detect-if-browser-window-is-scrolled-to-bottom
		return "return Math.ceil(window.innerHeight + window.scrollY + 1) >= document.body.scrollHeight;";
	}

	/**
	 * Sets the border for a specific chat message element identified by its
	 * messageId.
	 *
	 * @param messageId The unique identifier of the chat message element to set the
	 *                  border for.
	 * @return A JavaScript code snippet that adds the 'selected' class to the
	 *         specified chat message element.
	 */
	public String generateSetBorderScript(UUID messageId) {
		return "var element = document.getElementById('message-" + messageId.toString() + "');\n"
				+ "element.classList.add('selected');";
	}

	/**
	 * Removes the border from all chat message elements.
	 *
	 * @return A JavaScript code snippet that removes the 'selected' class from all
	 *         chat message elements.
	 */
	public String generateRemoveAllBordersScript() {
		return "var elements = document.getElementsByClassName('chat-bubble');\n"
				+ "for (var i = 0; i < elements.length; i++) {\n" + "    elements[i].classList.remove('selected');\n"
				+ "}";
	}
	
	/**
	 * Generates a JavaScript script that navigates back in the browser history.
	 * 
	 * @return A string containing the JavaScript code to execute. This script, when
	 *         executed in a browser context, navigates one step back in the browser's history.
	 */
	public String generateNavigateBackScript() {
	    return "window.history.back();";
	}

	/**
	 * Generates a JavaScript script that navigates forward in the browser history.
	 * 
	 * @return A string containing the JavaScript code to execute. This script, when
	 *         executed in a browser context, navigates one step forward in the browser's history.
	 */
	public String generateNavigateForwardScript() {
	    return "window.history.forward();";
	}

	/**
	 * Generates a JavaScript script that retrieves the current text selection from
	 * the browser.
	 * 
	 * @return A string containing the JavaScript code to execute. This script, when
	 *         executed in a browser context, returns the currently selected text as
	 *         a string.
	 */
	public String generateGetSelectionScript() {
		return "return window.getSelection().toString();";
	}

	/**
	 * Returns the CSS class for a given ChatMessage based on its role and content.
	 *
	 * @param message The ChatMessage object containing the message data.
	 * @return A string representing the CSS class for the given ChatMessage.
	 */
	private String getCssClassForRole(ChatMessage message) {
		String cssClass = "chat-bubble ";
		if (message.getRole() == ChatRole.USER) {
			cssClass += "user";
		} else if (message.getRole() == ChatRole.ASSISTANT) {
			cssClass += "assistant";
		} else if (message.getRole() == ChatRole.NOTIFICATION) {
			if (message.getMessage().startsWith("WARNING:") || message.getMessage().startsWith("ERROR:")) {
				cssClass += "error";
			} else {
				cssClass += "notification";
			}
		}
		return cssClass;
	}

	/**
	 * Substitutes the default and small font sizes in the CSS with the values from
	 * preferences.
	 *
	 * @param css The original CSS string containing the default font size values.
	 * @return A modified CSS string with updated font size values.
	 */
	private String substituteFontSizes(String css) {
		// TODO: Find a better way to substitute font sizes dynamically.
		return css.replace("--default-font-size: 14px", "--default-font-size: " + Preferences.getChatFontSize() + "px")
				.replace("--small-font-size: 10px",
						"--small-font-size: " + Preferences.getNotificationFontSize() + "px");
	}

	/**
	 * Loads and concatenates the contents of multiple CSS files.
	 *
	 * @param cssFiles An array of filenames for the CSS files to load.
	 * @return A string containing the concatenated contents of all specified CSS
	 *         files.
	 */
	private String loadCssFiles(String[] cssFiles) {
		StringBuilder css = new StringBuilder();
		for (String filename : cssFiles) {
			css.append(loadFile(Constants.CSS_PATH, filename));
			css.append("\n");
		}
		return css.toString();
	}

	/**
	 * Loads and concatenates the contents of multiple JavaScript files.
	 *
	 * @param jsFiles An array of filenames for the JavaScript files to load.
	 * @return A string containing the concatenated contents of all specified
	 *         JavaScript files.
	 */
	private String loadJsFiles(String[] jsFiles) {
		StringBuilder js = new StringBuilder();
		for (String filename : jsFiles) {
			js.append(loadFile(Constants.JS_PATH, filename));
			js.append("\n");
		}
		return js.toString();
	}

	/**
	 * Loads the contents of a file as a string.
	 *
	 * @param filepath The path to the file.
	 * @param filename The name of the file to load.
	 * @return A string containing the contents of the specified file.
	 */
	private String loadFile(String filepath, String filename) {
		try (InputStream in = FileLocator.toFileURL(new URL(filepath + filename)).openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}