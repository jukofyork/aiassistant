package eclipse.plugin.aiassistant.context;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.IndentationFormatter;
import eclipse.plugin.aiassistant.utility.LanguageFileExtensions;

/**
 * The Context class provides information about the current state of the Eclipse
 * environment. It includes details such as the active file, compiler warnings
 * and errors, user input, document text, clipboard text, selection text, and
 * line numbers.
 */
public class Context {
	
	private String userText = "";
	private String filename = "";
	private String language = "";
	private String tag = "";
	private String compilerWarnings = "";
	private String compilerErrors = "";
	private String documentText = "";
	private String clipboardText = "";
	private String selectionText = "";
	private String lineNumberDescription = "";
	private String documentationGenerator = "";

	/**
	 * Constructs a new Context object with the given text editor and user input.
	 *
	 * @param userText   The user input from the chat conversation view.
	 */
	public Context(String userText) {
		
		// Can get the user text even if not in a text editor.
		this.userText = userText;
		
		// Try to get the active text editor (or null if not text editor)).
		ITextEditor textEditor = Eclipse.getActiveTextEditor();
		
		// Can't get any of these unless from a text editor.
		if (textEditor != null) {
			
			// Save all dirty editors for Eclipse.getEditorText().
			Eclipse.saveAllEditors(false);
			
			IFile activeFile = Eclipse.getActiveFile(textEditor);
			this.filename = activeFile.getProjectRelativePath().toString();
			this.language = LanguageFileExtensions.getLanguageName(filename);
			this.tag = LanguageFileExtensions.getMarkdownTag(filename);
			this.compilerWarnings = Eclipse.getCompilerWarnings(activeFile);
			this.compilerErrors = Eclipse.getCompilerErrors(activeFile);
			this.documentText = Eclipse.getEditorText(activeFile);
	
			// NOTE: Deliberately not trimming to not effect line numbers and indentation.
			this.selectionText = Eclipse.getSelectedText(textEditor);
			if (!this.selectionText.isEmpty()) {
				Integer startLine = Eclipse.getSelectedStartLine(textEditor);
				Integer endLine = Eclipse.getSelectedEndLine(textEditor);
				if ((startLine - endLine) == 0) {	// Using == doesn't always work!?
					this.lineNumberDescription = "line " + startLine.toString();
				} else {
					this.lineNumberDescription = "lines " + startLine.toString() + " to " + endLine.toString();
				}
			}
			
			// TODO: Move out.
			if (language.equals("Java")) {
				documentationGenerator = "Javadoc (@see, @param, @return, @throws, etc)";
			}
			else if (language.equals("C++")) {
				documentationGenerator = "Doxygen (@see, @param, @return, @throws, etc)";
			}
			
		}
		
		// Can get the clipboard contents even if not in a text editor.
		// NOTE: Deliberately not trimming to not effect line numbers and indentation.
		this.clipboardText = Eclipse.getClipboardContents();
		
		// Remove the indentation from both.
		// May not work well if mixed space and tab indentation.
		this.clipboardText = IndentationFormatter.removeIndentation(this.clipboardText);
		this.selectionText = IndentationFormatter.removeIndentation(this.selectionText);

	}

	/**
	 * Returns the user input text from the chat conversation view.
	 * 
	 * @return The user input text from the chat conversation view.
	 */
	public String getUserText() {
		return userText;
	}

	/**
	 * Returns the filename of the active file.
	 * 
	 * @return The filename of the active file.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Returns the language name associated with the active file's extension.
	 * 
	 * @return The language name associated with the active file's extension.
	 */
	public String getLanguage() {
		return language;
	}
	
	/**
	 * Returns the Markdown language tag associated with the active file's extension.
	 * 
	 * @return The language tag associated with the active file's extension.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Returns a string containing all compiler warnings for the active file. Each
	 * warning is listed on a new line, along with the line number and filename
	 * where it occurred.
	 * 
	 * @return A string containing all compiler warnings for the active file.
	 */
	public String getCompilerWarnings() {
		return compilerWarnings;
	}

	/**
	 * Returns a string containing all compiler errors for the active file. Each
	 * error is listed on a new line, along with the line number and filename where
	 * it occurred.
	 * 
	 * @return A string containing all compiler errors for the active file.
	 */
	public String getCompilerErrors() {
		return compilerErrors;
	}

	/**
	 * Returns the text content of the active file.
	 * 
	 * @return The text content of the active file.
	 */
	public String getDocumentText() {
		return documentText;
	}

	/**
	 * Returns the text content of the clipboard.
	 * 
	 * @return The text content of the clipboard.
	 */
	public String getClipboardText() {
		return clipboardText;
	}

	/**
	 * Returns the selected text from the active file in the editor.
	 * 
	 * @return The selected text from the active file in the editor.
	 */
	public String getSelectionText() {
		return selectionText;
	}

	/**
	 * Returns a description of the line numbers associated with the selected text.
	 * If only one line is selected, it returns "line X", where X is the line
	 * number. If multiple lines are selected, it returns "lines X to Y", where X
	 * and Y are the start and end line numbers.
	 * 
	 * @return A description of the line numbers associated with the selected text.
	 */
	public String getLineNumberDescription() {
		return lineNumberDescription;
	}
	
	public String getDocumentationGenerator() {
		return documentationGenerator;
	}

}