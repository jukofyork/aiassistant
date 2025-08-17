package eclipse.plugin.aiassistant.utility;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

/**
 * A field editor for JSON/TOML type preferences that validates input as valid JSON or TOML.
 *
 * This class extends {@link StringFieldEditor} to provide validation for both JSON and TOML syntax.
 * Empty strings are allowed for cases where no configuration is needed.
 * Supports both JSON syntax ("key": "value") and TOML inline table syntax (key = "value").
 * Outer curly braces are automatically added during validation.
 *
 * @see StringFieldEditor
 */
public class JsonFieldEditor extends StringFieldEditor {

	/** ObjectMapper instance for JSON validation */
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/** ObjectMapper instance for TOML validation */
	private static final ObjectMapper TOML_MAPPER = new ObjectMapper(new TomlFactory());

	/**
	 * Creates a new JSON/TOML field editor with no initialization.
	 *
	 * This constructor is intended for use by subclasses only.
	 */
	protected JsonFieldEditor() {
	}

	/**
	 * Creates a JSON/TOML field editor with the given parameters.
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent composite where this field editor will be placed
	 */
	public JsonFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
		setEmptyStringAllowed(true);
		setErrorMessage("Input must be valid JSON (\"key\": \"value\") or TOML (key = \"value\") syntax, or empty.");
	}

	/**
	 * Sets the tool tip text of the label and text control.
	 *
	 * @param text the text to set
	 */
	public void setToolTipText(String text) {
		getLabelControl().setToolTipText(text);
		getTextControl().setToolTipText(text);
	}

	/**
	 * Checks if the input is valid JSON or TOML syntax, or empty.
	 * Automatically wraps the input with curly braces for validation.
	 *
	 * @return true if the input is valid JSON, valid TOML, or empty; false otherwise
	 */
	@Override
	protected boolean doCheckState() {
		String text = getTextControl().getText();

		// Allow empty or whitespace-only strings
		if (text == null || text.trim().isEmpty()) {
			return true;
		}

		// Wrap input with curly braces for validation
		String wrappedText = "{ " + text.trim() + " }";

		// Try JSON first
		try {
			JSON_MAPPER.readTree(wrappedText);
			return true;
		} catch (JsonProcessingException e) {
			// JSON parsing failed, try TOML
		}

		// Try TOML - wrap inline table in a document structure for validation
		try {
			String wrappedToml = "temp = " + wrappedText;
			TOML_MAPPER.readTree(wrappedToml);
			return true;
		} catch (JsonProcessingException e) {
			// Both JSON and TOML parsing failed
			return false;
		}
	}
}