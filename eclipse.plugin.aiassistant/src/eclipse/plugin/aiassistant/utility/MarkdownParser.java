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
 * Supports advanced features such as:
 * <ul>
 *   <li>Code blocks with language-specific syntax highlighting</li>
 *   <li>LaTeX mathematical expressions (inline and block)</li>
 *   <li>Standard Markdown formatting (headers, lists, bold, italic, etc.)</li>
 * </ul>
 * 
 * The parser handles nested blocks and ensures proper HTML escaping of content.
 * It also provides options for including interactive buttons for code blocks.
 */
public class MarkdownParser {

    /**
     * Converts Markdown formatted text to HTML with optional code block interaction buttons.
     *
     * @param markdownText The Markdown text to convert
     * @param includeCodeBlockButtons Whether to include copy/paste/review buttons for code blocks
     * @return HTML formatted string with preserved Markdown styling
     * @throws IllegalStateException If the scanner encounters errors while processing input
     */
    public static String convertMarkdownToHtml(String markdownText, boolean includeCodeBlockButtons) {
        markdownText = StringUtils.stripStart(markdownText, " ");
        StringBuilder htmlOutput = new StringBuilder();

        // Enum to track the current parsing context for handling nested blocks
        enum BlockType { NONE, CODE, LATEX }
        BlockType currentBlock = BlockType.NONE;

        // Regex patterns for identifying different block types
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

        StringBuilder latexBuffer = new StringBuilder();

        try (Scanner scanner = new Scanner(markdownText)) {
            scanner.useDelimiter("\n");

            while (scanner.hasNext()) {
                String line = scanner.next();
                
                Matcher codeBlockMatcher = codeBlockPattern.matcher(line);
                Matcher latexMultilineBlockOpenMatcher = latexMultilineBlockOpenPattern.matcher(line);            
                Matcher latexSinglelineBlockOpennMatcher = latexSinglelineBlockOpenPattern.matcher(line);
                Matcher latexCloseMatcher = latexBlockClosePattern.matcher(line);
                
                // Process the line based on the current block type
                switch (currentBlock) {
                    case NONE:
                        if (codeBlockMatcher.find()) {
                            String language = codeBlockMatcher.group(1);
                            appendOpenCodeBlock(htmlOutput, language, includeCodeBlockButtons);
                            currentBlock = BlockType.CODE;
                        } else if (latexMultilineBlockOpenMatcher.find()) {
                            appendOpenLatexBlock(htmlOutput);
                            String trimmedLine = line.replaceFirst("^\\s*(\\$\\$|\\\\\\[)\\s*", "");
                            latexBuffer.append(trimmedLine);
                            currentBlock = BlockType.LATEX;                        
                        } else if (latexSinglelineBlockOpennMatcher.find()) {
                            appendOpenLatexBlock(htmlOutput);
                            String trimmedLine = line.replaceFirst("^\\s*(\\$\\$|\\\\\\[)\\s*", "")
                                    .replaceAll("\\s*(\\$\\$|\\\\\\])$", "");
                            latexBuffer.append(trimmedLine);
                            appendLatexBufferAndClose(htmlOutput, latexBuffer);
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
                            String trimmedLine = line.replaceAll("\\s*(\\$\\$|\\\\\\])$", "");
                            latexBuffer.append(trimmedLine);
                            appendLatexBufferAndClose(htmlOutput, latexBuffer);
                            currentBlock = BlockType.NONE;
                        } else {
                            latexBuffer.append(line).append("\n");
                        }
                        break;
                }
            }
        }

        // Handle unclosed blocks at the end of the input
        switch (currentBlock) {
            case CODE:
                appendCloseCodeBlock(htmlOutput);
                break;
            case LATEX:
                appendLatexBufferAndClose(htmlOutput, latexBuffer);
                break;
            default:
                break;
        }

        return replaceEscapeCodes(replaceLineBreaks(htmlOutput.toString()));
    }

    /**
     * Converts a single line of text to HTML, handling both inline LaTeX and Markdown formatting.
     *
     * @param line The input line to convert
     * @return The converted HTML line
     */
    private static String convertLineToHtml(String line) {
        return convertMarkdownLineToHtml(convertInLineLatexToHtml(line));
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
     * Handles headers, unordered lists, horizontal rules, bold, italic, strikethrough, and inline code.
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

        // Convert inline code
        markdownLine = markdownLine.replaceAll("`(.*?)`", "<code><strong>$1</strong></code>");

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
        htmlOutput.append("""
                          <input type="${showCopy}" onClick="eclipseCopyCode(document.getElementById('${codeBlockId}').innerText)" value="Copy Code" />
                          <input type="${showCopy}" onClick="eclipseReplaceSelection(document.getElementById('${codeBlockId}').innerText)" value="Replace Selection" />
                          <input type="${showReviewChanges}" onClick="eclipseReviewChanges(document.getElementById('${codeBlockId}').innerText)" value="Review Changes"/>
                          <input type="${showApplyPatch}" onClick="eclipseApplyPatch(document.getElementById('${codeBlockId}').innerText)" value="Apply Patch"/>
                          <pre><code lang="${lang}" id="${codeBlockId}">"""
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
     * Appends the opening tag for a LaTeX block to the output.
     *
     * @param htmlOutput The StringBuilder object to append the opening tag to
     */
    private static void appendOpenLatexBlock(StringBuilder htmlOutput) {
        htmlOutput.append("<span class=\"block-latex\">");
    }
    
    /**
     * Appends the base64 encoded LaTeX content and closing tag to the output.
     *
     * @param htmlOutput The StringBuilder object to append the content and closing tag to
     * @param latexBuffer The StringBuilder containing the LaTeX content
     */
    private static void appendLatexBufferAndClose(StringBuilder htmlOutput, StringBuilder latexBuffer) {
        if (latexBuffer.length() > 0) {
            htmlOutput.append(Base64.getEncoder().encodeToString(latexBuffer.toString().getBytes()));
            latexBuffer.setLength(0);
        }
        htmlOutput.append("</span>\n");
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