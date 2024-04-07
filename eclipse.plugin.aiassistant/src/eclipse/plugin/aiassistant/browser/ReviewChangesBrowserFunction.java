package eclipse.plugin.aiassistant.browser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.utility.Eclipse;
import eclipse.plugin.aiassistant.utility.IndentationFormatter;

/**
 * This class represents a JavaScript function that allows users to review and
 * apply changes using a Compare Editor.
 */
public class ReviewChangesBrowserFunction extends DisableableBrowserFunction {

	private String newText;
	
	/**
	 * Constructs a new instance of the ReviewChangesFunction class.
	 *
	 * @param browser The browser in which this function is used.
	 * @param name    The name of this function.
	 */
	public ReviewChangesBrowserFunction(Browser browser, String name) {
		super(browser, name);
	}

	/**
	 * This method is called when the JavaScript function is invoked. It compares
	 * the old and new code blocks in the active text editor, shows a dialog for the
	 * user to review the changes, and replaces the selected code block with the
	 * user's reviewed version if provided.
	 *
	 * @param arguments The arguments passed to this function. The first argument
	 *                  should be a string containing the new code block to compare
	 *                  with the old one.
	 * @return Always returns null.
	 */
	@Override
	public Object function(Object[] arguments) {
		if (isEnabled() && arguments.length > 0 && arguments[0] instanceof String) {
			String newCodeBlock = (String) arguments[0];
			if (!newCodeBlock.isEmpty()) {
				ITextEditor textEditor = Eclipse.getActiveTextEditor();
				if (textEditor != null) {
					// Save all dirty editors for Eclipse.getSelectedTextOrEditorText().
					Eclipse.saveAllEditors(false);
					String oldCodeBlock = Eclipse.getSelectedTextOrEditorText(textEditor);
					if (!oldCodeBlock.isEmpty()) {
						newCodeBlock = IndentationFormatter.matchIndentation(oldCodeBlock, newCodeBlock);
						String replacementCodeBlock = showCompareDialog(oldCodeBlock,
								newCodeBlock);
						if (!replacementCodeBlock.isEmpty()) {
							Eclipse.replaceAndSelectText(textEditor, replacementCodeBlock);
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * This method opens a compare dialog with the selected text from the active
	 * editor and the provided right data. It creates two CompareItem objects, one
	 * for the left side (selected text) and one for the right side (provided data).
	 * It also creates a CompareConfiguration object and a MyCompareEditorInput
	 * object which are used to configure and provide input for the compare dialog.
	 * The method then opens the compare dialog using the CompareUI class and
	 * returns the new text.
	 * 
	 * @param rightData The data to be compared with the selected text from the
	 *                  active editor.
	 * @return The new text after the comparison.
	 */
	private String showCompareDialog(String leftData, String rightData) {

		// Clear ready to populate in MyCompareEditorInput.saveChanges().
		newText = "";

		CompareItem leftItem = new CompareItem("Left", leftData, System.currentTimeMillis(), true);
		CompareItem rightItem = new CompareItem("Right", rightData, System.currentTimeMillis(), false);
		CompareConfiguration compareConfiguration = new CompareConfiguration();
		compareConfiguration.setLeftLabel("Original Code");
		compareConfiguration.setRightLabel("Suggested Changes");
		URL leftImageUrl;
		URL rightImageUrl;
		try {
			leftImageUrl = new URL(Constants.ICONS_PATH + "CompareEdit.png");
			rightImageUrl = new URL(Constants.ICONS_PATH + "Robot.png");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		Image leftIconImage = ImageDescriptor.createFromURL(leftImageUrl).createImage();
		Image rightIconImage = ImageDescriptor.createFromURL(rightImageUrl).createImage();
		compareConfiguration.setLeftImage(leftIconImage);
		compareConfiguration.setRightImage(rightIconImage);
		MyCompareEditorInput compareEditorInput = new MyCompareEditorInput(compareConfiguration, leftItem, rightItem);
		compareEditorInput.setTitle("AI Assistant: Review Suggested Changes");
		CompareUI.openCompareDialog(compareEditorInput);
		leftIconImage.dispose();
		rightIconImage.dispose();
		return newText;
	}
	
	/**
	 * The CompareItem class represents an item that can be compared in a comparison
	 * dialog. It implements the IStreamContentAccessor, ITypedElement,
	 * IModificationDate, and IEditableContent interfaces.
	 */
	// https://stackoverflow.com/questions/8466464/example-of-compare-editor
	class CompareItem implements IStreamContentAccessor, ITypedElement, IModificationDate, IEditableContent {
		private String contents, name;
		private long time;
		private boolean isEditable;

		/**
		 * Constructs a new CompareItem with the given name, contents, modification
		 * date, and editable flag.
		 * 
		 * @param name       The name of the item.
		 * @param contents   The contents of the item as a string.
		 * @param time       The modification date of the item in milliseconds since the
		 *                   epoch.
		 * @param isEditable A flag indicating whether the item is editable.
		 */
		CompareItem(String name, String contents, long time, boolean isEditable) {
			this.name = name;
			this.contents = contents;
			this.time = time;
			this.isEditable = isEditable;
		}

		/**
		 * Returns an InputStream containing the contents of the item.
		 * 
		 * @return An InputStream containing the contents of the item.
		 * @throws CoreException If there is a problem getting the contents.
		 */
		@Override
		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		}

		/**
		 * Returns an Image representing the item.
		 * 
		 * @return An Image representing the item.
		 */
		@Override
		public Image getImage() {
			return null;
		}

		/**
		 * Returns the modification date of the item in milliseconds since the epoch.
		 * 
		 * @return The modification date of the item in milliseconds since the epoch.
		 */
		@Override
		public long getModificationDate() {
			return time;
		}

		/**
		 * Returns the name of the item.
		 * 
		 * @return The name of the item.
		 */
		@Override
		public String getName() {
			return name;
		}

		/**
		 * Returns the type of the item.
		 * 
		 * @return The type of the item.
		 */
		@Override
		public String getType() {
			return ITypedElement.TEXT_TYPE;
		}

		/**
		 * Returns a flag indicating whether the item is editable.
		 * 
		 * @return A flag indicating whether the item is editable.
		 */
		@Override
		public boolean isEditable() {
			return isEditable;
		}

		/**
		 * Sets the content of the item to the given byte array.
		 * 
		 * @param newContent The new content of the item as a byte array.
		 */
		@Override
		public void setContent(byte[] newContent) {
			contents = newContent.toString();
		}

		/**
		 * Replaces the destination element with the source element.
		 * 
		 * @param dest The destination element to replace.
		 * @param src  The source element to use as the replacement.
		 * @return The replaced element.
		 */
		@Override
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			return dest; // Not sure what this does.
		}

	}

	/**
	 * This class extends the CompareEditorInput class and provides a custom
	 * implementation. It includes a DiffNode object to store the differences
	 * between two CompareItems.
	 */
	class MyCompareEditorInput extends CompareEditorInput {
		private CompareItem leftItem, rightItem;
		private DiffNode diffNode;

		/**
		 * Constructor for MyCompareEditorInput.
		 * 
		 * @param compareConfiguration The configuration for the comparison.
		 * @param leftItem             The item to be compared on the left side.
		 * @param rightItem            The item to be compared on the right side.
		 */
		public MyCompareEditorInput(CompareConfiguration compareConfiguration, CompareItem leftItem,
				CompareItem rightItem) {
			super(compareConfiguration);
			this.leftItem = leftItem;
			this.rightItem = rightItem;
		}

		/**
		 * Prepares the input for comparison by creating a DiffNode and assigning it to
		 * the field.
		 * 
		 * @param monitor The progress monitor to show progress to the user.
		 * @return The prepared input for comparison.
		 * @throws InvocationTargetException If an exception occurs during the
		 *                                   invocation of a method.
		 * @throws InterruptedException      If the thread is interrupted while waiting.
		 */
		@Override
		protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			diffNode = new DiffNode(leftItem, rightItem); // Assign the DiffNode to the field
			return diffNode;
		}

		/**
		 * Saves the changes made in the comparison editor.
		 * 
		 * @param monitor The progress monitor to show progress to the user.
		 * @throws CoreException If a core exception occurs during the save operation.
		 */
		// https://www.eclipse.org/forums/index.php/t/107467/
		// https://stackoverflow.com/questions/8794830/get-document-from-compare-editor/16576615
		@Override
		public void saveChanges(IProgressMonitor monitor) throws CoreException {
			super.saveChanges(monitor);
			newText = CompareUI.getDocument(diffNode.getLeft()).get();
		}

	}
	
}