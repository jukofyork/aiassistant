package eclipse.plugin.aiassistant.utility;

import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility class for converting Markdown text to HTML format.
 * This class supports advanced features such as code blocks with syntax highlighting,
 * LaTeX mathematical expressions (both inline and block), and standard Markdown elements
 * like headers, lists, bold, and italic text. It handles nested blocks and ensures proper
 * HTML escaping to prevent XSS vulnerabilities.
 */
public class MarkdownParser {

    /**
     * Converts Markdown formatted text to HTML. This method also allows for the inclusion
     * of interactive buttons within code blocks, such as copy, paste, and review changes.
     *
     * @param markdownText The Markdown text to be converted.
     * @param includeCodeBlockButtons Flag to determine whether interactive buttons should be included.
     * @return A string representing the HTML formatted text.
     * @throws IllegalStateException If an error occurs during parsing, typically from malformed input.
     */
    public static String convertMarkdownToHtml(String markdownText, boolean includeCodeBlockButtons) {
        markdownText = StringUtils.stripStart(markdownText, " ");
        StringBuilder htmlOutput = new StringBuilder();

        // Enum to track the current parsing context for handling nested blocks
        enum BlockType { NONE, CODE, LATEX }
        BlockType currentBlock = BlockType.NONE;

        // Patterns to identify and process different types of blocks in Markdown.
        final Pattern codeBlockPattern = Pattern.compile("^[ \\t]*```([a-zA-Z]*)[ \\t]*$");
        final Pattern latexMultilineBlockOpenPattern = Pattern.compile(
            "^[ \\t]*(?:" +
            "\\$\\$(?!.*\\$\\$)|" +                           // $$ syntax without closing on same line
            "\\\\\\[(?!.*\\\\\\])" +                          // \[ syntax without closing on same line
            ").*$"
        );
        final Pattern latexSinglelineBlockOpenPattern = Pattern.compile(
            "^[ \\t]*(?:" +
            "\\$\\$(?:.*\\$\\$)|" +                           // $$ syntax with closing on same line
            "\\\\\\[(?:.*\\\\\\])" +                          // \[ syntax with closing on same line
            ").*$"
        );
        final Pattern latexBlockClosePattern = Pattern.compile("^.*?(\\$\\$|\\\\\\])[ \\t]*$");
        
        final Pattern thinkingBlockOpenPattern = Pattern.compile("<thinking>");        
        final Pattern thinkingBlockClosePattern = Pattern.compile("</thinking>");

        StringBuilder latexBlockBuffer = new StringBuilder();
        
        int thinkingBlockCount = 0;
        
        int currentQuoteLevel = 0;

        try (Scanner scanner = new Scanner(markdownText)) {
            scanner.useDelimiter("\n");

            while (scanner.hasNext()) {
                String line = scanner.next();
                
            	Matcher thinkingBlockOpenMatcher = thinkingBlockOpenPattern.matcher(line);
            	Matcher thinkingBlockCloseMatcher = thinkingBlockClosePattern.matcher(line);
            	
				Matcher codeBlockMatcher = codeBlockPattern.matcher(line);
				Matcher latexMultilineBlockOpenMatcher = latexMultilineBlockOpenPattern.matcher(line);            
				Matcher latexSinglelineBlockOpennMatcher = latexSinglelineBlockOpenPattern.matcher(line);
				Matcher latexCloseMatcher = latexBlockClosePattern.matcher(line);
                
                switch (currentBlock) {
                    case NONE:
                    	while (thinkingBlockOpenMatcher.find()) {
                    	    htmlOutput.append(getThinkingBlockOpeningHtml());
                    	    line = thinkingBlockOpenMatcher.replaceFirst("");
                    	    thinkingBlockOpenMatcher = thinkingBlockOpenPattern.matcher(line);
                    	    thinkingBlockCount++;
                    	}
                    	while (thinkingBlockCloseMatcher.find()) {
                    	    htmlOutput.append(getThinkingBlockClosingHtml());
                    	    line = thinkingBlockCloseMatcher.replaceFirst("");
                    	    thinkingBlockCloseMatcher = thinkingBlockClosePattern.matcher(line);
                    	    thinkingBlockCount++;
                    	}
                    	
                        // Get quote level for current line
                        int lineQuoteLevel = 0;
                        if (line.trim().startsWith(">")) {
                            lineQuoteLevel = countQuoteMarkers(line);
                            // Remove quote markers if present and trim
                            line = line.replaceFirst("^[ \\t]*>+[ \\t]*", "");
                        } else {
                            // Not a quote line - close all quote blocks
                            while (currentQuoteLevel > 0) {
                                htmlOutput.append("</blockquote>");
                                currentQuoteLevel--;
                            }
                        }
                        
                        // Handle quote level transitions
                        while (currentQuoteLevel > lineQuoteLevel) {
                            htmlOutput.append("</blockquote>");
                            currentQuoteLevel--;
                        }
                        while (currentQuoteLevel < lineQuoteLevel) {
                            htmlOutput.append("<blockquote>");
                            currentQuoteLevel++;
                        }
                    	
                        if (codeBlockMatcher.find()) {
                            String language = codeBlockMatcher.group(1);
                            appendOpenCodeBlock(htmlOutput, language, includeCodeBlockButtons);
                            currentBlock = BlockType.CODE;
                        } else if (latexMultilineBlockOpenMatcher.find()) {
                            String latexLine = line.replaceFirst("^\\s*(\\$\\$|\\\\\\[)\\s*", "");
                            latexBlockBuffer.append(latexLine);
                            currentBlock = BlockType.LATEX;                        
                        } else if (latexSinglelineBlockOpennMatcher.find()) {
                            String latexLine = line.replaceFirst("^\\s*(\\$\\$|\\\\\\[)\\s*", "")
                                    .replaceAll("\\s*(\\$\\$|\\\\\\])$", "");
                            latexBlockBuffer.append(latexLine);
                            flushLatexBlockBuffer(latexBlockBuffer, htmlOutput);
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
                            String latexLine = line.replaceAll("\\s*(\\$\\$|\\\\\\])$", "");
                            latexBlockBuffer.append(latexLine);
                            flushLatexBlockBuffer(latexBlockBuffer, htmlOutput);
                            currentBlock = BlockType.NONE;
                        } else {
                        	latexBlockBuffer.append(line + "\n");
                        }
                        break;
                }
            }
        }

        // Handle unclosed blocks at the end of the input
        // NOTE: Don't auto-close LaTeX here - it causes flicker/errors in output.
        if (currentBlock == BlockType.CODE) {
        	appendCloseCodeBlock(htmlOutput);
        }

        // Close any remaining quote levels
        while (currentQuoteLevel > 0) {
            htmlOutput.append("</blockquote>");
            currentQuoteLevel--;
        }
        
        // Close any unclosed thinking blocks
        while (thinkingBlockCount > 0) {
        	htmlOutput.append(getThinkingBlockClosingHtml());
        	thinkingBlockCount--;
        }

        return replaceEscapeCodes(trimThinkingyBlock(replaceLineBreaks(htmlOutput.toString())));
    }
    
    /**
     * Generates HTML markup for opening a collapsible UI section used to show AI thinking process.
     * Creates a nested structure of div, details, and summary elements for interactive display.
     *
     * @return HTML markup string for opening a collapsible thinking block
     */
    private static String getThinkingBlockOpeningHtml() {
        return "<div class=\"thinking\"><details><summary>Thinking...</summary>";
    }
    
    /**
     * Generates HTML markup for closing a collapsible thinking block section.
     * Must be paired with a corresponding opening markup to maintain proper HTML structure.
     *
     * @return HTML markup string for closing a collapsible thinking block
     */
    private static String getThinkingBlockClosingHtml() {
        return "</details></div>";
    }
    
    /**
     * Normalizes whitespace and line breaks around thinking block elements for consistent display.
     * Ensures clean formatting by removing excess whitespace and standardizing break tags.
     *
     * @param html The HTML string containing thinking block markup to normalize
     * @return The HTML string with standardized spacing around thinking blocks
     */
    private static String trimThinkingyBlock(String html) {
        return html
                .replaceAll("</summary>(?:\\s|<br/>)+", "</summary><br/>")
                .replaceAll("</details>\\s*</div>(?:\\s|<br/>)+", "</details></div><br/>");
    }
    
    /**
     * Counts the number of nested quote levels (>) at the start of a Markdown line.
     * Used for parsing nested blockquotes in Markdown text.
     *
     * @param line The text line to analyze for quote markers
     * @return The number of nested quote levels found
     */
    private static int countQuoteMarkers(String line) {
        int count = 0;
        String trimmed = line.trim();
        while (trimmed.startsWith(">")) {
            count++;
            trimmed = trimmed.substring(1).trim();
        }
        return count;
    }

    /**
     * Converts a single line of text to HTML, processing inline elements in a specific order:
     * inline code first, then LaTeX expressions, and finally Markdown formatting. This order
     * prevents interference between different syntax patterns and ensures proper escaping.
     *
     * @param line The input line containing any combination of inline code (`code`),
     *             LaTeX ($math$), and Markdown formatting
     * @return The HTML-formatted line with all inline elements converted to appropriate
     *         HTML spans with base64 encoded content
     */
    private static String convertLineToHtml(String line) {
        return convertMarkdownLineToHtml(convertInLineLatexToHtml(convertInlineCodeToHtml(line)));
    }

    /**
     * Converts Markdown inline code segments to HTML spans with base64 encoded content.
     * Processes text enclosed in single backticks (`code`) and transforms them into
     * HTML spans with the content base64 encoded to preserve special characters.
     *
     * @param line Text line potentially containing inline code segments
     * @return Line with inline code converted to HTML spans containing base64 encoded content
     */
    private static String convertInlineCodeToHtml(String line) {
        Pattern inlineCodePattern = Pattern.compile("`(.*?)`");
        return inlineCodePattern.matcher(line).replaceAll(match -> {
            String content = match.group(1);
            String base64Content = Base64.getEncoder().encodeToString(content.getBytes());
            return "<span class=\"inline-code\">" + base64Content + "</span>";
        });
    }

    /**
     * Converts inline LaTeX expressions to HTML spans with base64 encoded content.
     * Handles both $...$ and \(...\) syntax for inline math.
     *
     * @param line Text line potentially containing inline LaTeX
     * @return Line with LaTeX expressions converted to HTML spans
     */
    private static String convertInLineLatexToHtml(String line) {
        String inlineLatexPatterns = 
                "\\$(.*?)\\$|" +                                // Single $ pairs
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
     * Handles headers, unordered lists, horizontal rules, bold, italic and strikethrough.
     *
     * @param markdownLine The input Markdown line
     * @return The converted HTML line
     */
    private static String convertMarkdownLineToHtml(String markdownLine) {
    	
        // Convert headers (h1 to h6)
		markdownLine = markdownLine.replaceAll("^[ \\t]*# (.*?)$", "<h1>$1</h1>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*## (.*?)$", "<h2>$1</h2>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*### (.*?)$", "<h3>$1</h3>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*#### (.*?)$", "<h4>$1</h4>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*##### (.*?)$", "<h5>$1</h5>");
		markdownLine = markdownLine.replaceAll("^[ \\t]*###### (.*?)$", "<h6>$1</h6>");

        // Convert unordered lists
        markdownLine = markdownLine.replaceAll("^[ \\t]*[*+-] (.*?)$", "&#8226; $1");

        // Convert horizontal rules
        markdownLine = markdownLine.replaceAll("^[ \\t]*(?:-{3,}|\\*{3,}|_{3,})$", "<hr>");

        // Convert bold, italic, and combinations
        markdownLine = markdownLine.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b><i>$1</i></b>");
        markdownLine = markdownLine.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        markdownLine = markdownLine.replaceAll("\\*(.*?)\\*", "<i>$1</i>");

        // Convert strikethrough
        markdownLine = markdownLine.replaceAll("~~(.*?)~~", "<del>$1</del>");

        // Add line break
        markdownLine += "<br/>";

        return markdownLine;
    }

    /**
     * Converts a single line of text to an HTML-escaped line with escaped backslashes.
     * Adds a newline character at the end of the processed line.
     *
     * @param line The line of text to be processed
     * @return The HTML-escaped line with backslashes escaped and a newline character appended
     */
    private static String convertToEscapedHtmlLine(String line) {
        return StringEscapeUtils.escapeHtml4(escapeBackslashes(line)) + "\n";
    }

    /**
     * Appends HTML markup for a code block with optional interaction buttons.
     * Generates a unique ID for each code block to support button functionality.
     *
     * @param htmlOutput StringBuilder to append HTML markup to
     * @param language Programming language identifier for syntax highlighting
     * @param includeCodeBlockButtons Whether to include interaction buttons
     */
	private static void appendOpenCodeBlock(StringBuilder htmlOutput, String language, boolean includeCodeBlockButtons) {
	    String codeBlockId = UUID.randomUUID().toString();
	    String copyIcon = Eclipse.loadIconAsBase64("CopyToClipboard.png");
	    String replaceIcon = Eclipse.loadIconAsBase64("ReplaceSelection.png");
	    String reviewIcon = Eclipse.loadIconAsBase64("ReviewChanges.png");
	    htmlOutput.append("""
	                      <style>
	                      .copy-btn { background-image: url(data:image/png;base64,%s); }
	                      .replace-btn { background-image: url(data:image/png;base64,%s); }
	                      .review-btn { background-image: url(data:image/png;base64,%s); }
	                      .patch-btn { background-image: url(data:image/png;base64,%s); }
	                      </style>
	                      <input type="${showCopy}" class="code-button copy-btn" onClick="eclipseCopyCode(getSelectedTextFromElement('${codeBlockId}'))" value="Copy Code" />
	                      <input type="${showCopy}" class="code-button replace-btn" onClick="eclipseReplaceSelection(getSelectedTextFromElement('${codeBlockId}'))" value="Replace Selection" />
	                      <input type="${showReviewChanges}" class="code-button review-btn" onClick="eclipseReviewChanges(getSelectedTextFromElement('${codeBlockId}'))" value="Review Changes"/>
	                      <input type="${showApplyPatch}" class="code-button patch-btn" onClick="eclipseApplyPatch(document.getElementById('${codeBlockId}').innerText)" value="Apply Patch"/>
	                      <pre><code lang="${lang}" id="${codeBlockId}">"""
	            .formatted(copyIcon, replaceIcon, reviewIcon, reviewIcon)
	            .replace( "${lang}", language )
	            .replace( "${codeBlockId}", codeBlockId )
	            .replace( "${showCopy}", includeCodeBlockButtons ? "button" : "hidden" )
	            .replace( "${showReviewChanges}", includeCodeBlockButtons && !"diff".equals(language) ? "button" : "hidden" )
	            .replace( "${showApplyPatch}", includeCodeBlockButtons && "diff".equals(language) ? "button" : "hidden" )                    
	            );
	}

    /**
     * Appends the closing tags for a code block to the output.
     *
     * @param htmlOutput The StringBuilder object to append the closing tags to
     */
    private static void appendCloseCodeBlock(StringBuilder htmlOutput) {
        htmlOutput.append("</code></pre>").append("\n");
    }
        
    /**
     * Flushes the accumulated LaTeX content from the buffer into the HTML output.
     * This method wraps the LaTeX content in a {@code <span>} element with a class for styling.
     * The content is Base64 encoded to ensure that any special characters are preserved
     * and do not interfere with the HTML structure.
     *
     * @param latexBlockBuffer The buffer containing the accumulated LaTeX content.
     * @param htmlOutput The StringBuilder to which the HTML content is appended.
     */
    private static void flushLatexBlockBuffer(StringBuilder latexBlockBuffer, StringBuilder htmlOutput) {
        if (latexBlockBuffer.length() > 0) {
            htmlOutput.append("<span class=\"block-latex\">");
            htmlOutput.append(Base64.getEncoder().encodeToString(latexBlockBuffer.toString().getBytes()));
            htmlOutput.append("</span>\n");
            latexBlockBuffer.setLength(0);  // Clear the buffer after processing to avoid duplicate content.
        }
    }

    /**
     * Escapes any backslashes in the given input string.
     *
     * @param input The input string to escape backslashes in
     * @return The escaped input string
     */
    private static String escapeBackslashes(String input) {
        return input.replace("\\", "\\\\");
    }

    /**
     * Replaces special characters in the HTML string with their escaped versions.
     * 
     * @param html The HTML string to escape special characters in
     * @return The HTML string with special characters escaped
     */
    private static String replaceEscapeCodes(String html) {
        return html.replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n");
    }

    /**
     * Removes MSDOS style line breaks (carriage returns) from the HTML string.
     * 
     * @param html The HTML string to remove carriage returns from
     * @return The HTML string with carriage returns removed
     */
    private static String replaceLineBreaks(String html) {
        return html.replace("\r", "");
    }
}