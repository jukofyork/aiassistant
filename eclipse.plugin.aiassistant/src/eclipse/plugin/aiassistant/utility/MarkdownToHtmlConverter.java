package eclipse.plugin.aiassistant.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public class MarkdownToHtmlConverter {

	/**
	 * Represents parsed information from a code block delimiter line.
	 * Used to track backtick count for proper nesting and language for syntax highlighting.
	 *
	 * @param numBackticks Number of consecutive backticks (3 or more for valid code blocks)
	 * @param language Programming language identifier (empty string for closing delimiters)
	 */
	private record CodeBlockInfo(int numBackticks, String language) {}

	private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^[ \\t]*(`{3,})([a-zA-Z]*)[ \\t]*$");
	private static final Pattern CODE_INLINE_PATTERN = Pattern.compile("`(.*?)`");
	private static final Pattern LATEX_INLINE_PATTERN = Pattern.compile("\\$(.*?)\\$|\\\\\\((.*?)\\\\\\)");
	private static final Pattern LATEX_MULTILINE_BLOCK_OPEN_PATTERN = Pattern.compile("^[ \\t]*(?:\\$\\$(?!.*\\$\\$)|\\\\\\[(?!.*\\\\\\])).*$");
	private static final Pattern LATEX_SINGLELINE_BLOCK_OPEN_PATTERN = Pattern.compile("^[ \\t]*(?:\\$\\$(?:.*\\$\\$)|\\\\\\[(?:.*\\\\\\])).*$");
	private static final Pattern LATEX_BLOCK_CLOSE_PATTERN = Pattern.compile("^.*?(\\$\\$|\\\\\\])[ \\t]*$");
	private static final Pattern LATEX_LINE_START_PATTERN = Pattern.compile("^\\s*(\\$\\$|\\\\\\[)\\s*");
	private static final Pattern LATEX_LINE_END_PATTERN = Pattern.compile("\\s*(\\$\\$|\\\\\\])$");
	private static final Pattern THINKING_BLOCK_OPEN_PATTERN = Pattern.compile("<think>");
	private static final Pattern THINKING_BLOCK_CLOSE_PATTERN = Pattern.compile("</think>");
	private static final Pattern THINKING_BLOCK_SUMMARY_CLEANUP_PATTERN = Pattern.compile("</summary>(?:\\s|<br/>)+");
	private static final Pattern THINKING_BLOCK_DETAILS_CLEANUP_PATTERN = Pattern.compile("</details>\\s*</div>(?:\\s|<br/>)+");
	private static final Pattern QUOTE_MARKER_PATTERN = Pattern.compile("^[ \\t]*>+[ \\t]*");
	private static final Pattern HEADER_H1_PATTERN = Pattern.compile("^[ \\t]*# (.*?)$");
	private static final Pattern HEADER_H2_PATTERN = Pattern.compile("^[ \\t]*## (.*?)$");
	private static final Pattern HEADER_H3_PATTERN = Pattern.compile("^[ \\t]*### (.*?)$");
	private static final Pattern HEADER_H4_PATTERN = Pattern.compile("^[ \\t]*#### (.*?)$");
	private static final Pattern HEADER_H5_PATTERN = Pattern.compile("^[ \\t]*##### (.*?)$");
	private static final Pattern HEADER_H6_PATTERN = Pattern.compile("^[ \\t]*###### (.*?)$");
	private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^[ \\t]*[*+-] (.*?)$");
	private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^[ \\t]*(?:-{3,}|\\*{3,}|_{3,})$");
	private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("\\*\\*\\*(.*?)\\*\\*\\*");
	private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
	private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
	private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.*?)~~");

	// The types of bullet points to use for unordered lists [•, ◦, ▪].
	private static final String[] BULLET_SYMBOLS = { "&#8226;", "&#9702;", "&#9642;"};

	// Load these once to re-use inside appendOpenCodeBlock.
	private static final String COPY_ICON = Eclipse.loadIconAsBase64("CopyToClipboard.png");
	private static final String REPLACE_ICON = Eclipse.loadIconAsBase64("ReplaceSelection.png");
	private static final String REVIEW_ICON = Eclipse.loadIconAsBase64("ReviewChanges.png");

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
		// HTML usually expands relative to markdown; pre-size to avoid resizing
		StringBuilder htmlOutput = new StringBuilder(markdownText.length() * 2);

		enum BlockType { NONE, CODE, LATEX }
		BlockType currentBlock = BlockType.NONE;

		StringBuilder latexBlockBuffer = new StringBuilder();

		int thinkingBlockCount = 0;
		boolean hasThinkingBlocks = false;

		int currentQuoteLevel = 0;

		// Primitive int stack: zero boxing overhead, no synchronization
		int[] codeBlockUnwindStack = new int[8];
		int codeBlockStackSize = 0;

		try (BufferedReader reader = new BufferedReader(new StringReader(markdownText))) {
			String line;
			while ((line = reader.readLine()) != null) {

				switch (currentBlock) {
				case NONE: {
					Matcher thinkingBlockOpenMatcher = THINKING_BLOCK_OPEN_PATTERN.matcher(line);
					Matcher thinkingBlockCloseMatcher = THINKING_BLOCK_CLOSE_PATTERN.matcher(line);
					Matcher latexMultilineBlockOpenMatcher = LATEX_MULTILINE_BLOCK_OPEN_PATTERN.matcher(line);
					Matcher latexSinglelineBlockOpenMatcher = LATEX_SINGLELINE_BLOCK_OPEN_PATTERN.matcher(line);

					while (thinkingBlockOpenMatcher.find()) {
						htmlOutput.append(getThinkingBlockOpeningHtml());
						hasThinkingBlocks = true;
						line = replaceFirstPattern(line, THINKING_BLOCK_OPEN_PATTERN, "");
						thinkingBlockOpenMatcher = THINKING_BLOCK_OPEN_PATTERN.matcher(line);
						thinkingBlockCount++;
					}
					while (thinkingBlockCloseMatcher.find()) {
						htmlOutput.append(getThinkingBlockClosingHtml());
						line = replaceFirstPattern(line, THINKING_BLOCK_CLOSE_PATTERN, "");
						thinkingBlockCloseMatcher = THINKING_BLOCK_CLOSE_PATTERN.matcher(line);
						thinkingBlockCount--;
					}

					int lineQuoteLevel = 0;
					if (isQuoteLine(line)) {
						lineQuoteLevel = countQuoteMarkers(line);
						line = replaceFirstPattern(line, QUOTE_MARKER_PATTERN, "");
					} else {
						while (currentQuoteLevel > 0) {
							htmlOutput.append("</blockquote>");
							currentQuoteLevel--;
						}
					}

					while (currentQuoteLevel > lineQuoteLevel) {
						htmlOutput.append("</blockquote>");
						currentQuoteLevel--;
					}
					while (currentQuoteLevel < lineQuoteLevel) {
						htmlOutput.append("<blockquote>");
						currentQuoteLevel++;
					}

					CodeBlockInfo openCodeBlockInfo = getCodeBlockInfo(line);
					if (openCodeBlockInfo.numBackticks > 0) {
						appendOpenCodeBlock(htmlOutput, openCodeBlockInfo.language, includeCodeBlockButtons);
						if (codeBlockStackSize == codeBlockUnwindStack.length) {
							codeBlockUnwindStack = java.util.Arrays.copyOf(codeBlockUnwindStack, codeBlockStackSize * 2);
						}
						codeBlockUnwindStack[codeBlockStackSize++] = openCodeBlockInfo.numBackticks;
						currentBlock = BlockType.CODE;
					} else if (latexMultilineBlockOpenMatcher.find()) {
						String latexLine = replaceFirstPattern(line, LATEX_LINE_START_PATTERN, "");
						latexBlockBuffer.append(latexLine);
						currentBlock = BlockType.LATEX;
					} else if (latexSinglelineBlockOpenMatcher.find()) {
						String latexLine = replaceFirstPattern(line, LATEX_LINE_START_PATTERN, "");
						latexLine = replaceFirstPattern(latexLine, LATEX_LINE_END_PATTERN, "");
						latexBlockBuffer.append(latexLine);
						flushLatexBlockBuffer(latexBlockBuffer, htmlOutput);
					} else {
						String escapedLine = line;
						if (containsHtmlMetaChars(escapedLine)) {
							escapedLine = StringEscapeUtils.escapeHtml4(escapedLine);
						}
						htmlOutput.append(convertLineToHtml(escapedLine));
					}
					break;
				}
				case CODE: {
					CodeBlockInfo codeBlockInfo = getCodeBlockInfo(line);
					if (codeBlockInfo.numBackticks > 0 && codeBlockInfo.language.isEmpty()) {
						if (codeBlockStackSize > 0 && codeBlockInfo.numBackticks == codeBlockUnwindStack[codeBlockStackSize - 1]) {
							if (--codeBlockStackSize == 0) {
								appendCloseCodeBlock(htmlOutput);
								currentBlock = BlockType.NONE;
							}
						}
					}
					if (currentBlock == BlockType.CODE) {
						htmlOutput.append(convertToEscapedHtmlLine(line)).append("\n");
					}
					break;
				}
				case LATEX: {
					Matcher latexCloseMatcher = LATEX_BLOCK_CLOSE_PATTERN.matcher(line);
					if (latexCloseMatcher.find()) {
						String latexLine = replaceFirstPattern(line, LATEX_LINE_END_PATTERN, "");
						latexBlockBuffer.append(latexLine);
						flushLatexBlockBuffer(latexBlockBuffer, htmlOutput);
						currentBlock = BlockType.NONE;
					} else {
						latexBlockBuffer.append(line).append("\n");
					}
					break;
				}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Error reading markdown input", e);
		}

		if (currentBlock == BlockType.CODE) {
			appendCloseCodeBlock(htmlOutput);
		}

		while (currentQuoteLevel > 0) {
			htmlOutput.append("</blockquote>");
			currentQuoteLevel--;
		}

		while (thinkingBlockCount > 0) {
			htmlOutput.append(getThinkingBlockClosingHtml());
			thinkingBlockCount--;
		}

		String result = htmlOutput.toString();
		if (hasThinkingBlocks) {
			result = trimThinkingyBlock(result);
		}
		return replaceEscapeCodes(result);
	}

	/**
	 * Generates HTML markup for opening a collapsible UI section used to show AI thinking process.
	 * Creates a nested structure of div, details, and summary elements for interactive display.
	 *
	 * @return HTML markup string for opening a collapsible thinking block
	 */
	private static String getThinkingBlockOpeningHtml() {
		return "<div class=\"thinking\"><details open><summary>Thinking...</summary>";
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
		html = replacePattern(html, THINKING_BLOCK_SUMMARY_CLEANUP_PATTERN, "</summary><br/>");
		html = replacePattern(html, THINKING_BLOCK_DETAILS_CLEANUP_PATTERN, "</details></div><br/>");
		return html;
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
		// Guard inline parsers: if the trigger characters aren't present, skip the regex entirely
		if (line.indexOf('`') >= 0) {
			line = convertInlineCodeToHtml(line);
		}
		if (line.indexOf('$') >= 0 || line.indexOf('\\') >= 0) {
			line = convertInLineLatexToHtml(line);
		}
		return convertMarkdownLineToHtml(line);
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
		return CODE_INLINE_PATTERN.matcher(line).replaceAll(match -> {
			String content = match.group(1);
			String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
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
		return LATEX_INLINE_PATTERN.matcher(line).replaceAll(match -> {
			// Check each capture group since we don't know which pattern matched
			for (int i = 1; i <= match.groupCount(); i++) {
				String content = match.group(i);
				if (content != null) {
					String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
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
		// Headers: only run the 6 regexes when a '#' is actually present
		if (markdownLine.indexOf('#') >= 0) {
			markdownLine = replacePattern(markdownLine, HEADER_H1_PATTERN, "<h1>$1</h1>");
			markdownLine = replacePattern(markdownLine, HEADER_H2_PATTERN, "<h2>$1</h2>");
			markdownLine = replacePattern(markdownLine, HEADER_H3_PATTERN, "<h3>$1</h3>");
			markdownLine = replacePattern(markdownLine, HEADER_H4_PATTERN, "<h4>$1</h4>");
			markdownLine = replacePattern(markdownLine, HEADER_H5_PATTERN, "<h5>$1</h5>");
			markdownLine = replacePattern(markdownLine, HEADER_H6_PATTERN, "<h6>$1</h6>");
		}

		// Lists: only attempt when a list marker character exists
		if (markdownLine.indexOf('*') >= 0 || markdownLine.indexOf('+') >= 0 || markdownLine.indexOf('-') >= 0) {
			Matcher listMatcher = UNORDERED_LIST_PATTERN.matcher(markdownLine);
			if (listMatcher.matches()) {
				int indentLevel = 0;
				for (int i = 0; i < markdownLine.length(); i++) {
					char ch = markdownLine.charAt(i);
					if (ch == ' ' || ch == '\t') {
						indentLevel++;
					} else {
						break;
					}
				}
				String indentSpaces = "&#160;".repeat(Math.max(0, indentLevel) * 2);
				String bullet = BULLET_SYMBOLS[(indentLevel / 2) % BULLET_SYMBOLS.length];
				markdownLine = "&#8201;" + indentSpaces + bullet + " " + listMatcher.group(1);
			}
		}

		// Horizontal rules
		if (markdownLine.indexOf('-') >= 0 || markdownLine.indexOf('*') >= 0 || markdownLine.indexOf('_') >= 0) {
			markdownLine = replacePattern(markdownLine, HORIZONTAL_RULE_PATTERN, "<hr>");
		}

		// Bold / italic
		if (markdownLine.indexOf('*') >= 0) {
			markdownLine = replacePattern(markdownLine, BOLD_ITALIC_PATTERN, "<b><i>$1</i></b>");
			markdownLine = replacePattern(markdownLine, BOLD_PATTERN, "<b>$1</b>");
			markdownLine = replacePattern(markdownLine, ITALIC_PATTERN, "<i>$1</i>");
		}

		// Strikethrough
		if (markdownLine.indexOf('~') >= 0) {
			markdownLine = replacePattern(markdownLine, STRIKETHROUGH_PATTERN, "<del>$1</del>");
		}

		markdownLine += "<br/>";
		return markdownLine;
	}

	/**
	 * Converts a single line of text to an HTML-escaped line with escaped backslashes.
	 * Skips work when the line contains no characters that need escaping.
	 *
	 * @param line The line of text to be processed
	 * @return The HTML-escaped line with backslashes escaped
	 */
	private static String convertToEscapedHtmlLine(String line) {
		if (line.indexOf('\\') >= 0) {
			line = escapeBackslashes(line);
		}
		if (containsHtmlMetaChars(line)) {
			line = StringEscapeUtils.escapeHtml4(line);
		}
		return line;
	}

	/**
	 * Parses a line to extract code block delimiter information.
	 * Matches lines with 3+ backticks and optional language specifier.
	 *
	 * @param line The text line to analyze
	 * @return CodeBlockInfo with backtick count and language, or (0, "") if not a code block delimiter
	 */
	private static CodeBlockInfo getCodeBlockInfo(String line) {
		// Fast path: if there's no backtick at all, it can't be a code fence
		if (line.indexOf('`') < 0) {
			return new CodeBlockInfo(0, "");
		}
		Matcher matcher = CODE_BLOCK_PATTERN.matcher(line);
		if (matcher.matches()) {
			return new CodeBlockInfo(matcher.group(1).length(), matcher.group(2));
		}
		return new CodeBlockInfo(0, "");
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
				.formatted(COPY_ICON, REPLACE_ICON, REVIEW_ICON, REVIEW_ICON)
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
			htmlOutput.append(Base64.getEncoder().encodeToString(latexBlockBuffer.toString().getBytes(StandardCharsets.UTF_8)));
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
	 * Replaces all occurrences of a specified pattern in the input string with the given replacement.
	 *
	 * @param input The original string where replacements are to be made.
	 * @param pattern The regular expression pattern to search for in the input string.
	 * @param replacement The string to replace each match of the pattern.
	 * @return A new string with all occurrences of the pattern replaced by the replacement string.
	 */
	private static String replacePattern(String input, Pattern pattern, String replacement) {
		Matcher matcher = pattern.matcher(input);
		return matcher.replaceAll(replacement);
	}

	/**
	 * Replaces the first occurrence of a specified pattern in the input string with the given replacement.
	 *
	 * @param input The original string where the replacement is to be made.
	 * @param pattern The regular expression pattern to search for in the input string.
	 * @param replacement The string to replace the first match of the pattern.
	 * @return A new string with the first occurrence of the pattern replaced by the replacement string.
	 */
	private static String replaceFirstPattern(String input, Pattern pattern, String replacement) {
		Matcher matcher = pattern.matcher(input);
		return matcher.replaceFirst(replacement);
	}

	/**
	 * Escapes quotes and newlines so the HTML can be safely embedded inside a
	 * JavaScript string literal.
	 *
	 * @param html The HTML string to escape
	 * @return The escaped string with {@code "}, {@code '} and newlines backslash-escaped
	 */
	private static String replaceEscapeCodes(String html) {
		return html.replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n");
	}

	/**
	 * Returns {@code true} if the string contains any character that would be
	 * transformed by {@link StringEscapeUtils#escapeHtml4(String)}.
	 *
	 * @param s The string to inspect
	 * @return {@code true} if the string contains HTML metacharacters
	 */
	private static boolean containsHtmlMetaChars(String s) {
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fast, zero-allocation check that returns {@code true} if the first
	 * non-whitespace character in the line is a {@code '>'} quote marker.
	 *
	 * @param line The text line to analyze
	 * @return {@code true} if the line begins with a quote marker
	 */
	private static boolean isQuoteLine(String line) {
		for (int i = 0, n = line.length(); i < n; i++) {
			char c = line.charAt(i);
			if (c == '>') return true;
			if (c != ' ' && c != '\t') return false;
		}
		return false;
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
		int len = line.length();
		int i = 0;

		// skip leading whitespace
		while (i < len && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
			i++;
		}

		// count '>' markers, allowing whitespace between them
		while (i < len && line.charAt(i) == '>') {
			count++;
			i++;
			while (i < len && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
				i++;
			}
		}
		return count;
	}
}