package eclipse.plugin.aiassistant.utility;

/**
 * The IndentationFormatter class provides utility methods to format code
 * snippets by removing or matching indentation.
 */
public class IndentationFormatter {

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private IndentationFormatter() {
	}

	/**
	 * Removes the common whitespace prefix from each line in the provided code
	 * snippet (perhaps should use String.stipIndent() instead?).
	 * 
	 * @param codeSnippet The code snippet to remove indentation from.
	 * @return The code snippet with the common whitespace prefix removed from each
	 *         line.
	 */
	public static String removeIndentation(String codeSnippet) {
		String[] lines = splitLines(codeSnippet);
		int longestCommonPrefixLength = findLongestCommonWhitespacePrefix(lines).length();
		for (int i = 0; i < lines.length; i++) {
			int beginIndex = Math.min(longestCommonPrefixLength, lines[i].length());
			lines[i] = lines[i].substring(beginIndex);
		}
		return String.join("\n", lines);
	}

	/**
	 * Matches the indentation of the target code snippet to the source code
	 * snippet.
	 * 
	 * @param sourceCodeSnippet The source code snippet to match the indentation
	 *                          from.
	 * @param targetCodeSnippet The target code snippet to match the indentation to.
	 * @return The target code snippet with the indentation matched to the source
	 *         code snippet.
	 */
	public static String matchIndentation(String sourceCodeSnippet, String targetCodeSnippet) {
		String[] sourceLines = splitLines(sourceCodeSnippet);
		String[] targetLines = splitLines(targetCodeSnippet);
		String sourcelongestCommonPrefix = findLongestCommonWhitespacePrefix(sourceLines);
		String targetlongestCommonPrefix = findLongestCommonWhitespacePrefix(targetLines);
		for (int i = 0; i < targetLines.length; i++) {
			int beginIndex = Math.min(targetlongestCommonPrefix.length(), targetLines[i].length());
			targetLines[i] = targetLines[i].substring(beginIndex);
			targetLines[i] = sourcelongestCommonPrefix + targetLines[i];
		}
		return String.join("\n", targetLines);
	}

	/**
	 * Finds the longest common whitespace prefix among all non-blank lines in an
	 * array of strings. The prefix is defined as a sequence of spaces or tabs that
	 * appear at the start of each line.
	 * 
	 * @param lines An array of strings to search for the common whitespace prefix.
	 * @return The longest common whitespace prefix among all non-blank lines in the
	 *         input array.
	 */
	private static String findLongestCommonWhitespacePrefix(String[] lines) {
		String currentPrefix = "";
		for (int charIndex = 0;; charIndex++) {
			char currentChar = 0;
			for (String line : lines) {
				if (!line.trim().isEmpty()) { // Skip blank lines.
					if (charIndex >= line.length()
							|| (line.charAt(charIndex) != ' ' && line.charAt(charIndex) != '\t')) {
						return currentPrefix;
					} else if (currentChar == 0) {
						currentChar = line.charAt(charIndex);
					} else if (line.charAt(charIndex) != currentChar) {
						return currentPrefix;
					}
				}
			}
			if (currentChar == 0) {
				return currentPrefix; // All lines are blank/skipped.
			}
			currentPrefix += currentChar;
		}
	}

	/**
	 * Splits a given code snippet into individual lines.
	 * 
	 * @param codeSnippet The code snippet to be split into lines.
	 * @return An array of strings where each string is a line from the code
	 *         snippet.
	 */
	private static String[] splitLines(String codeSnippet) {
		return removeDosLineBreaks(codeSnippet).split("\n");
	}

	/**
	 * Removes any DOS-style line breaks (carriage returns) from a given code
	 * snippet.
	 * 
	 * @param codeSnippet The code snippet to be processed.
	 * @return The code snippet with all DOS-style line breaks removed.
	 */
	private static String removeDosLineBreaks(String codeSnippet) {
		return codeSnippet.replace("\r", "");
	}

}
