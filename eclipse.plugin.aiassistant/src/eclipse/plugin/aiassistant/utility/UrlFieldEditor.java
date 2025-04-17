package eclipse.plugin.aiassistant.utility;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * The `URLFieldEditor` class is a custom field editor for URL inputs in Eclipse
 * plugins. It extends the `StringFieldEditor` class and provides additional
 * functionality to validate if the input is a valid OpenAI compatible API endpoint.
 */
public class UrlFieldEditor extends StringFieldEditor {

	/**
	 * Constructs a new URLFieldEditor with the specified name, label text, and
	 * parent.
	 *
	 * @param name      the name of the field editor
	 * @param labelText the label text for the field editor
	 * @param parent    the parent composite for the field editor
	 */
	public UrlFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
		setEmptyStringAllowed(false);
		setErrorMessage("URL must be a valid OpenAI compatible API endpoint, ending with \"/v1\".");
	}
	
	/**
	 * Sets the tool tip text of the label and text control.
	 * 
	 * @param text
	 *            the text to set
	 */
	public void setToolTipText(String text) {
		getLabelControl().setToolTipText(text);
		getTextControl().setToolTipText(text);
	}

	/**
	 * Checks if the input in the URL field is a valid web URL with just a port
	 * number.
	 * 
	 * @return True if the input is a valid URL, false otherwise.
	 */
	@Override
	protected boolean doCheckState() {
		String text = getTextControl().getText();
		if (text != null && text.length() > 0) {
			try {
				URL url = new URL(text);
				if ((url.getProtocol().equals("http") || url.getProtocol().equals("https")) 
						&& url.getFile().endsWith("/v1")) {
					return true;
				}
			} catch (MalformedURLException e) {
				return false;
			}
		}
		return false;
	}
	
}