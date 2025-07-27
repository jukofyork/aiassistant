package eclipse.plugin.aiassistant.prompt;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.FileLocator;
import org.stringtemplate.v4.ST;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.context.Context;
import eclipse.plugin.aiassistant.preferences.Preferences;

/**
 * PromptLoader is responsible for loading and processing prompt messages used
 * in the plugin.
 */
public class PromptLoader {

	/**
	 * Creates predefined prompt messages based on the given type and context.
	 *
	 * @param type    The type of prompt message to create.
	 * @param context The context object containing relevant information for
	 *                generating the prompt.
	 * @return An array of predefined prompt messages.
	 */
	public static String[] createPredefinedPromptMessage(Prompts type, Context context) {

		// Load the prompt template.
		String promptText = Preferences.getDefault().getString(type.preferenceName());

		// Transform the "<<switch-roles>>" strings to "[[switch-roles]]".
		promptText = applySwitchRolesTransform(promptText);

		// Perform the substitutions on the prompt template.
		promptText = applySubstitutions(promptText,
				"taskname", type.getTaskName(),
				"usertext", context.getUserText(),
				"filename", context.getFilename(),
				"language", context.getLanguage(),
				"tag", context.getTag(),
				"warnings", context.getCompilerWarnings(),
				"errors", context.getCompilerErrors(),
				"document", context.getDocumentText(),
				"clipboard", context.getClipboardText(),
				"selection", context.getSelectionText(),
				"lines", context.getLineNumberDescription(),
				"documentation", context.getDocumentationGenerator(),
				"file_diff", context.getFileDiff(),
				"staged_diff", context.getStagedDiff());

		// Un-escape the angle brackets.
		promptText = unescapeAngleBrackets(promptText);

		// Finally attempt to split into a messages (and auto-replies).
		return splitIntoMessages(promptText);

	}

	/**
	 * Loads the raw prompt message from the specified resource file.
	 *
	 * @param resourceFile The name of the resource file containing the raw prompt
	 *                     message.
	 * @return The raw prompt message as a string.
	 */
	public static String getRawPrompt(String resourceFile) {
		try (var in = FileLocator.toFileURL(new URL(Constants.PROMPTS_PATH + resourceFile)).openStream();
				var dis = new DataInputStream(in);) {
			var prompt = new String(dis.readAllBytes(), StandardCharsets.UTF_8);
			return prompt;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the system message prompt text from the preference store.
	 *
	 * @return The system prompt text as a string.
	 */
	public static String getSystemPromptText() {
		return Preferences.getDefault().getString(Prompts.SYSTEM.preferenceName());
	}

	/**
	 * Retrieves the developer message prompt text from the preference store.
	 *
	 * @return The developer prompt text as a string.
	 */
	public static String getDeveloperPromptText() {
		return Preferences.getDefault().getString(Prompts.DEVELOPER.preferenceName());
	}

	/**
	 * Applies the switch roles transform to the given prompt text.
	 *
	 * @param promptText The prompt text to transform.
	 * @return The transformed prompt text.
	 */
	private static String applySwitchRolesTransform(String promptText) {
		return promptText.replaceAll("<<switch-roles>>", "[[switch-roles]]");
	}

	/**
	 * Applies substitutions to the given prompt text using the specified key-value
	 * pairs.
	 * See: https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
	 *
	 * @param promptText    The prompt text to apply substitutions to.
	 * @param substitutions The key-value pairs for substitution.
	 * @return The prompt text with applied substitutions.
	 */
	private static String applySubstitutions(String promptText, String... substitutions) {
		if (substitutions.length % 2 != 0) {
			throw new IllegalArgumentException("Expecting key, value pairs");
		}
		ST st = new ST(promptText);
		for (int i = 0; i < substitutions.length; i += 2) {
			String substitutionTargetString = nullifyIfEmpty(escapeAngleBrackets(substitutions[i + 1]));
			st.add(substitutions[i], substitutionTargetString);
		}
		return st.render();
	}

	/**
	 * Splits the given input string into an array of messages based on the
	 * "[[switch-roles]]" delimiter.
	 *
	 * @param input The input string to split.
	 * @return An array of messages.
	 */
	public static String[] splitIntoMessages(String input) {
		String[] outputs = input.split("\\[\\[switch\\-roles\\]\\]", -1);
		for (int i = 0; i < outputs.length; i++) {
			outputs[i] = outputs[i].trim();
		}
		return outputs;
	}

	/**
	 * Escapes angle brackets in the given input string.
	 *
	 * @param input The input string to escape angle brackets in.
	 * @return The input string with escaped angle brackets.
	 */
	private static String escapeAngleBrackets(String input) {
		return input.replace("<", "\\<").replace(">", "\\>");
	}

	/**
	 * Unescapes angle brackets in the given input string.
	 *
	 * @param input The input string to unescape angle brackets in.
	 * @return The input string with unescaped angle brackets.
	 */
	private static String unescapeAngleBrackets(String input) {
		return input.replace("\\<", "<").replace("\\>", ">");
	}

	// https://github.com/antlr/stringtemplate4/blob/master/doc/null-vs-empty.md
	private static String nullifyIfEmpty(String input) {
		return input.isEmpty() ? null : input;
	}

}