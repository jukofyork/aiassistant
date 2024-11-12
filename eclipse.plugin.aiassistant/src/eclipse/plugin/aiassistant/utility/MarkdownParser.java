package eclipse.plugin.aiassistant.utility;

import java.util.Base64;
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
	    markdownText = StringUtils.stripStart(markdownText, " ");
	    StringBuilder htmlOutput = new StringBuilder();
	
	    // Track which block we entered first (if nested)
	    enum BlockType { NONE, CODE, LATEX }
	    BlockType currentBlock = BlockType.NONE;
	
	    final Pattern codeBlockPattern = Pattern.compile("^[ \\t]*```([a-zA-Z]*)[ \\t]*$");
	    final Pattern latexOpenPattern = Pattern.compile(
            "^[ \\t]*(?:" +
            "\\$\\$(?!.*\\$\\$)|" +                           // $$ syntax without closing on same line
            "\\\\\\[(?!.*\\\\\\])" +                          // \[ syntax without closing on same line
            ").*$"
	    );
	    final Pattern latexClosePattern = Pattern.compile("^.*?(\\$\\$|\\\\\\])[ \\t]*$");
	    
	    StringBuilder latexBuffer = new StringBuilder();
	    	
	    try (Scanner scanner = new Scanner(markdownText)) {
	        scanner.useDelimiter("\n");
	
	        while (scanner.hasNext()) {
	            String line = scanner.next();
	            Matcher codeBlockMatcher = codeBlockPattern.matcher(line);
	            Matcher latexOpenMatcher = latexOpenPattern.matcher(line);
	            Matcher latexCloseMatcher = latexClosePattern.matcher(line);
	            
	            switch (currentBlock) {
	                case NONE:
	                    if (codeBlockMatcher.find()) {
	                        String language = codeBlockMatcher.group(1);
	                        appendOpenCodeBlock(htmlOutput, language, includeCodeBlockButtons);
	                        currentBlock = BlockType.CODE;
	                    } else if (latexOpenMatcher.find()) {
	                        appendOpenLatexBlock(htmlOutput);
	                        String lineWithoutDelimite = line.replaceFirst("^\\s*(\\$\\$|\\\\\\[)\\s*", "");
	                        latexBuffer.append(lineWithoutDelimite);
	                        currentBlock = BlockType.LATEX;
	                    } else {
	                        htmlOutput.append(convertLineToHtml(StringEscapeUtils.escapeHtml4(line)));
	                    }
	                    break;
	            
	                case CODE:
	                    if (codeBlockMatcher.find()) {
	                        appendCloseCodeBlock(htmlOutput);
	                        currentBlock = BlockType.NONE;
	                    } else {
	                        htmlOutput.append(convertToEscapedHtmlLine(line));
	                    }
	                    break;
	            
	                case LATEX:
	                    if (latexCloseMatcher.find()) {
	                    	String lineWithoutDelimiter = line.replaceAll("\\s*(\\$\\$|\\\\\\])$", "");
	                        latexBuffer.append(lineWithoutDelimiter);
	                        htmlOutput.append(Base64.getEncoder().encodeToString(latexBuffer.toString().getBytes()));
	                        latexBuffer.setLength(0);
	                        appendCloseLatexBlock(htmlOutput);
	                        currentBlock = BlockType.NONE;
	                    } else {
	                        latexBuffer.append(line).append("\n");
	                    }
	                    break;
	            }
	        }
	    }
	
	    // Handle unclosed blocks
	    switch (currentBlock) {
	        case CODE:
	            appendCloseCodeBlock(htmlOutput);
	            break;
	        case LATEX:
	        	htmlOutput.append(Base64.getEncoder().encodeToString(latexBuffer.toString().getBytes()));
	            appendCloseLatexBlock(htmlOutput);
	            break;
	        default:
	            break;
	    }
	
	    return replaceEscapeCodes(replaceLineBreaks(htmlOutput.toString()));
	}

	private static String convertLineToHtml(String line) {
	    return convertMarkdownLineToHtml(convertSingleLineInLineLatexToHtml(convertSingleLineBlockLatexToHtml(line)));
	}

	private static String convertSingleLineBlockLatexToHtml(String line) {
		String singleLineBlockLatexPatterns = 
		        "\\$\\$(.*?)\\$\\$|" +                           // Double $$ pairs
		        "\\\\\\[(.*?)\\\\\\]";                           // \[ \] pairs
	    
	    Pattern singleLineBlockLatexPattern = Pattern.compile(singleLineBlockLatexPatterns);
	    return singleLineBlockLatexPattern.matcher(line).replaceAll(match -> {
	        // Check each capture group since we don't know which pattern matched
	        for (int i = 1; i <= match.groupCount(); i++) {
	            String content = match.group(i);
	            if (content != null) {
	                String base64Content = Base64.getEncoder().encodeToString(content.getBytes());
	                return "<span class=\"block-latex\">" + base64Content + "</span>";
	            }
	        }
	        return match.group(); // fallback, shouldn't happen
	    });
	}

	private static String convertSingleLineInLineLatexToHtml(String line) {
		String inlineLatexPatterns = 
		        "\\$(.*?)\\$|" +                                 // Single $ pairs
		        "\\\\\\((.*?)\\\\\\)";                          // \( \) pairs
	    
	    Pattern inlineLatexPattern = Pattern.compile(inlineLatexPatterns);
	    return inlineLatexPattern.matcher(line).replaceAll(match -> {
	        // Check each capture group since we don't know which pattern matched
	        for (int i = 1; i <= match.groupCount(); i++) {
	            String content = match.group(i);
	            if (content != null) {
	                String base64Content = Base64.getEncoder().encodeToString(content.getBytes());
	                return "<span class=\"inline-latex\">" + base64Content + "</span>";
	            }
	        }
	        return match.group(); // fallback, shouldn't happen
	    });
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
	
	private static void appendOpenLatexBlock(StringBuilder htmlOutput) {
	    htmlOutput.append("<span class=\"block-latex\">");
	}
	
	// For the appendCloseLatexBlock method:
	private static void appendCloseLatexBlock(StringBuilder htmlOutput) {
	    htmlOutput.append("</span>\n");
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