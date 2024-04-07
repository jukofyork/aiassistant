package eclipse.plugin.aiassistant.utility;

import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * A utility class for parsing and converting a text prompt to an HTML formatted
 * string.
 */
public class MarkdownParser {
		
	/**
	 * Converts the prompt text to an HTML formatted string.
	 *
	 * @return An HTML formatted string representation of the prompt text.
	 */
	public static String parseToHtml(String markdownString, boolean showCodeBlockButtons) {
		markdownString = StringUtils.stripStart(markdownString, " ");	// Strip leading spaces.
		var out = new StringBuilder();
		boolean isInsideCodeBlock = false;
		try (var scanner = new Scanner(markdownString)) {
			scanner.useDelimiter("\n");
			var codeBlockPattern = Pattern.compile("^```([aA-zZ]*)$");
			while (scanner.hasNext()) {
				var line = scanner.next();
				var codeBlockMatcher = codeBlockPattern.matcher(line);				
				if (codeBlockMatcher.find()) {
					if (!isInsideCodeBlock) {
						var language = codeBlockMatcher.group(1);
						openCodeBlock(out, language, showCodeBlockButtons);
						isInsideCodeBlock = true;
					}
					else {
						closeCodeBlock(out);
						out.append("\n");
						isInsideCodeBlock = false;
					}
				} 
				else {
					if (!isInsideCodeBlock) {
						out.append(MarkdownParser.parseMarkdown(StringEscapeUtils.escapeHtml4(line)));
						out.append("<br/>");
					}
					else {
						out.append(StringEscapeUtils.escapeHtml4(escapeBackSlashes(line)));
						if (scanner.hasNext()) {
							out.append("\n");
						}
						else {
							closeCodeBlock(out);
							isInsideCodeBlock = false;
						}
					}
				}
			}
		}
		return fixEscapeCodes(fixLineBreaks(out.toString()));
	}
	
	/**
	 * Converts a given input string in Markdown format to an HTML string.
	 *
	 *  @param input The input string in Markdown format.
	 *  @return The converted HTML string.
	 */
	private static String parseMarkdown(String input) {

		// Replace headers with <h> tags.
		// NOTE: <h1> looks too big and <h5>/<h6> look too small.
		input = input.replaceAll("^# (.*?)$", "<h2>$1</h2>");
		input = input.replaceAll("^## (.*?)$", "<h3>$1</h3>");
		input = input.replaceAll("^### (.*?)$", "<h4>$1</h4>");

	    // Replace unordered lists with bullet symbols.
		// NOTE: Using <ul> and <li> tags looks worse than this.
		input = input.replaceAll("^[*+-] (.*?)$", "&#8226; $1");

		// Replace horizontal rules with <hr> tag.
		input = input.replaceAll("^(?:-{3,}|\\*{3,}|_{3,})$", "<hr>");

		// Replace bold and italic text with <b> and <i> tags.
		input = input.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b><i>$1</i></b>");
		input = input.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
		input = input.replaceAll("\\*(.*?)\\*", "<i>$1</i>");

		// Replace inline code with <code> and <strong> tags.
		input = input.replaceAll("`(.*?)`", "<code><strong>$1</strong></code>");

		// Replace strikethrough text with <del> tag.
		input = input.replaceAll("~~(.*?)~~", "<del>$1</del>");
		
		return input;
	}
	
	/**
	 * Opens a new code block and appends it to the given output string builder.
	 *
	 * @param out The StringBuilder object to append the code block to.
	 * @param language The programming language of the code block.
	 * @param showCodeBlockButtons Whether to show the code block buttons or not.
	 */
	private static void openCodeBlock(StringBuilder out, String language, boolean showCodeBlockButtons) {
		// Generate a unique ID for the code block
		String codeBlockId = UUID.randomUUID().toString();
		out.append("""
				   <input type="${showCopy}" onClick="eclipseCopyCode(document.getElementById('${codeBlockId}').innerText)" value="Copy Code" />
				   <input type="${showCopy}" onClick="eclipseReplaceSelection(document.getElementById('${codeBlockId}').innerText)" value="Replace Selection" />
				   <input type="${showReviewChanges}" onClick="eclipseReviewChanges(document.getElementById('${codeBlockId}').innerText)" value="Review Changes"/>
				   <input type="${showApplyPatch}" onClick="eclipseApplyPatch(document.getElementById('${codeBlockId}').innerText)" value="Apply Patch"/>
				   <pre><code lang="${lang}" id="${codeBlockId}">"""
				.replace( "${lang}", language )
				.replace( "${codeBlockId}", codeBlockId )
				.replace( "${showCopy}", showCodeBlockButtons ? "button" : "hidden" )
				.replace( "${showReviewChanges}", showCodeBlockButtons&& !"diff".equals(language) ? "button" : "hidden" )
				.replace( "${showApplyPatch}", showCodeBlockButtons && "diff".equals(language) ? "button" : "hidden" )                    
				);
	}

	/**
	 * Closes the current code block and appends it to the given output string
	 * builder.
	 *
	 * @param out The StringBuilder object to append the closing tag of the code
	 *            block to.
	 */
	private static void closeCodeBlock(StringBuilder out) {
		out.append("</code></pre>");
	}

	/**
	 * Escapes any backslashes in the given input string.
	 *
	 * @param input The input string to escape backslashes in.
	 * @return The escaped input string.
	 */
	private static String escapeBackSlashes(String input) {
		return input.replace("\\", "\\\\");
	}

	/**
	 * Replaces escape codes in the HTML string.
	 * 
	 * @param html The HTML string to replace escape codes in.
	 * @return The HTML string with escape codes replaced.
	 */
	private static String fixEscapeCodes(String html) {
		return html.replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n");
	}

	/**
	 * Removes MSDOS style linebreaks from the HTML string.
	 * 
	 * @param html The HTML string to remove line breaks from.
	 * @return The HTML string with MSDOS style line breaks removed.
	 */
	private static String fixLineBreaks(String html) {
		return html.replace("\r", "");
	}

}
