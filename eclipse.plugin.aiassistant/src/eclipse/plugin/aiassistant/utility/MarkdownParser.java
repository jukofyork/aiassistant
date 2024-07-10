package eclipse.plugin.aiassistant.utility;

import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * A utility class for parsing and converting a text prompt to an HTML formatted
 * string.
 */
public class MarkdownParser {

	/**
	 * Converts Markdown text to an HTML formatted string.
	 *
	 * @return An HTML formatted string representation of the Markdown text.
	 */
	public static String convertMarkdownToHtml(String markdownText, boolean includeCodeBlockButtons) {
		markdownText = StringUtils.stripStart(markdownText, " "); // Strip leading spaces.

		StringBuilder htmlOutput = new StringBuilder();

		boolean isInsideCodeBlock = false;

		final Pattern codeBlockPattern = Pattern.compile("^[ \\t]*```([a-zA-Z]*)[ \\t]*$");

		try (Scanner scanner = new Scanner(markdownText)) {
			scanner.useDelimiter("\n");

			while (scanner.hasNext()) {
				String line = scanner.next();
				Matcher codeBlockMatcher = codeBlockPattern.matcher(line);
				if (!isInsideCodeBlock) {
					if (codeBlockMatcher.find()) {
						String language = codeBlockMatcher.group(1);
						appendOpenCodeBlock(htmlOutput, language, includeCodeBlockButtons);
						isInsideCodeBlock = true;
					} else {
						htmlOutput.append(convertMarkdownLineToHtml(StringEscapeUtils.escapeHtml4(line)));
					}
				} else {
					if (codeBlockMatcher.find()) {
						appendCloseCodeBlock(htmlOutput);
						isInsideCodeBlock = false;
					} else {
						htmlOutput.append(convertToEscapedHtmlLine(line));
					}
				}
			}
		}

		if (isInsideCodeBlock) {
			appendCloseCodeBlock(htmlOutput);
		}

		return replaceEscapeCodes(replaceLineBreaks(htmlOutput.toString()));
	}

	/**
	 * Converts a single line of Markdown to HTML.
	 *
	 * @param markdownLine The input Markdown line.
	 * @return The converted HTML line.
	 */
	private static String convertMarkdownLineToHtml(String markdownLine) {

		// Replace headers with <h> tags.
		markdownLine = markdownLine.replaceAll("^[ \\t]*# (.*?)$", "<h1>$1</h1>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*## (.*?)$", "<h2>$1</h2>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*### (.*?)$", "<h3>$1</h3>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*#### (.*?)$", "<h4>$1</h4>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*##### (.*?)$", "<h5>$1</h5>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*###### (.*?)$", "<h6>$1</h6>");

		// Replace unordered lists with bullet symbols.
		markdownLine = markdownLine.replaceAll("^[ \\t]*[*+-] (.*?)$", "&#8226; $1");

		// Replace horizontal rules with <hr> tag.
		markdownLine = markdownLine.replaceAll("^[ \\t]*(?:-{3,}|\\*{3,}|_{3,})$", "<hr>");

		// Replace bold and italic text with <b> and <i> tags.
		markdownLine = markdownLine.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b><i>$1</i></b>");
		markdownLine = markdownLine.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
		markdownLine = markdownLine.replaceAll("\\*(.*?)\\*", "<i>$1</i>");

		// Replace strikethrough text with <del> tag.
		markdownLine = markdownLine.replaceAll("~~(.*?)~~", "<del>$1</del>");

		// Replace inline code with <code> and <strong> tags.
		markdownLine = markdownLine.replaceAll("`(.*?)`", "<code><strong>$1</strong></code>");
		
		// Replace links with <a> tags.
		markdownLine = markdownLine.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\"$2\">$1</a>");

		// Replace images with <img> tags.
		markdownLine = markdownLine.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<img src=\"$2\" alt=\"$1\">");

		// Append the HTML line-break to the end of the line.
		markdownLine += "<br/>";

		return markdownLine;
	}

	/**
	 * Converts a single line of text to an HTML-escaped line with escaped
	 * backslashes. Adds a newline character at the end of the processed line.
	 *
	 * @param line The line of text to be processed.
	 * @return The HTML-escaped line with backslashes escaped and a newline
	 *         character appended.
	 */
	private static String convertToEscapedHtmlLine(String line) {
		return StringEscapeUtils.escapeHtml4(escapeBackslashes(line)) + "\n";
	}

	/**
	 * Appends the opening tags for a code block to the output.
	 *
	 * @param htmlOutput              The StringBuilder object to append the code
	 *                                block to.
	 * @param language                The programming language of the code block.
	 * @param includeCodeBlockButtons Whether to show the code block buttons or not.
	 */
	private static void appendOpenCodeBlock(StringBuilder htmlOutput, String language, boolean includeCodeBlockButtons) {
		// Generate a unique ID for the code block
		String codeBlockId = UUID.randomUUID().toString();
		htmlOutput.append("""
				   		  <input type="${showCopy}" onClick="eclipseCopyCode(document.getElementById('${codeBlockId}').innerText)" value="Copy Code" />
				   		  <input type="${showCopy}" onClick="eclipseReplaceSelection(document.getElementById('${codeBlockId}').innerText)" value="Replace Selection" />
				   		  <input type="${showReviewChanges}" onClick="eclipseReviewChanges(document.getElementById('${codeBlockId}').innerText)" value="Review Changes"/>
				   		  <input type="${showApplyPatch}" onClick="eclipseApplyPatch(document.getElementById('${codeBlockId}').innerText)" value="Apply Patch"/>
				   		  <pre><code lang="${lang}" id="${codeBlockId}">"""
				.replace( "${lang}", language )
				.replace( "${codeBlockId}", codeBlockId )
				.replace( "${showCopy}", includeCodeBlockButtons ? "button" : "hidden" )
				.replace( "${showReviewChanges}", includeCodeBlockButtons&& !"diff".equals(language) ? "button" : "hidden" )
				.replace( "${showApplyPatch}", includeCodeBlockButtons && "diff".equals(language) ? "button" : "hidden" )                    
				);
	}

	/**
	 * Appends the closing tags for a code block to the output.
	 *
	 * @param htmlOutput The StringBuilder object to append the closing tag of the
	 *                   code block to.
	 */
	private static void appendCloseCodeBlock(StringBuilder htmlOutput) {
		htmlOutput.append("</code></pre>").append("\n");
	}

	/**
	 * Escapes any backslashes in the given input string.
	 *
	 * @param input The input string to escape backslashes in.
	 * @return The escaped input string.
	 */
	private static String escapeBackslashes(String input) {
		return input.replace("\\", "\\\\");
	}

	/**
	 * Replaces escape codes in the HTML string.
	 * 
	 * @param html The HTML string to replace escape codes in.
	 * @return The HTML string with escape codes replaced.
	 */
	private static String replaceEscapeCodes(String html) {
		return html.replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n");
	}

	/**
	 * Removes MSDOS style linebreaks from the HTML string.
	 * 
	 * @param html The HTML string to remove line breaks from.
	 * @return The HTML string with MSDOS style line breaks removed.
	 */
	private static String replaceLineBreaks(String html) {
		return html.replace("\r", "");
	}

}