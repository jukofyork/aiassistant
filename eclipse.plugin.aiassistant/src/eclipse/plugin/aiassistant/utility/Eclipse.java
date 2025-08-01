package eclipse.plugin.aiassistant.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;

/**
 * This class provides utility methods for interacting with Eclipse IDE.
 */
public class Eclipse {

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private Eclipse() {
	}

	/**
	 * Returns the workbench instance.
	 *
	 * @return the workbench instance
	 */
	public static IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	/**
	 * Returns the display instance.
	 *
	 * @return the display instance
	 */
	public static Display getDisplay() {
		return PlatformUI.getWorkbench().getDisplay();
	}

	/**
	 * Returns the active workbench window.
	 *
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getWorkbench().getActiveWorkbenchWindow();
	}

	/**
	 * Returns the all workbench windows.
	 *
	 * @return the workbench windows
	 */
	public static IWorkbenchWindow[] getWorkbenchWindows() {
		return getWorkbench().getWorkbenchWindows();
	}

	/**
	 * Returns the shell for the active workbench window.
	 *
	 * @return the shell for the active workbench window.
	 */
	public static Shell getShell() {
		return getActiveWorkbenchWindow().getShell();
	}

	/**
	 * Returns the active page in the active workbench window.
	 *
	 * @return the active page
	 */
	public static IWorkbenchPage getActivePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}

	/**
	 * Returns the active part in the active page.
	 *
	 * @return the active part
	 */
	public static IWorkbenchPart getActivePart() {
		return getActivePage().getActivePart();
	}

	/**
	 * Returns the active editor in the active page.
	 *
	 * @return the active editor
	 */
	public static IEditorPart getActiveEditor() {
		return getActivePage().getActiveEditor();
	}

	/**
	 * Returns the active resource from the active editor.
	 *
	 * @return the active resource or null if not found
	 */
	public static IResource getActiveResource() {
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor != null) {
			return activeEditor.getEditorInput().getAdapter(IResource.class);
		}
		return null;
	}

	/**
	 * Returns the active text editor if the active editor is a text editor,
	 * otherwise null.
	 *
	 * @return the active text editor or null
	 */
	public static ITextEditor getActiveTextEditor() {
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor instanceof ITextEditor) {
			return (ITextEditor) activeEditor;
		}
		return null;
	}

	/**
	 * Returns the active file in the active text editor.
	 *
	 * @param textEditor the active text editor
	 * @return the active file, or null if not available
	 */
	public static IFile getActiveFile(ITextEditor textEditor) {
		if (textEditor == null || textEditor.getEditorInput() == null) {
			return null;
		}
		return textEditor.getEditorInput().getAdapter(IFile.class);
	}

	/**
	 * Returns the active project in the active text editor.
	 *
	 * @param textEditor the active text editor
	 * @return the active project, or null if not available
	 */
	public static IProject getActiveProject(ITextEditor textEditor) {
		IFile activeFile = getActiveFile(textEditor);
		if (activeFile == null) {
			return null;
		}
		return activeFile.getProject();
	}

	/**
	 * Returns the selected text in the active text editor.
	 *
	 * @param textEditor the active text editor
	 * @return the selected text
	 */
	public static String getSelectedText(ITextEditor textEditor) {
		ITextSelection textSelection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
		return textSelection.getText();
	}

	/**
	 * Returns the start line of the selected text in the active text editor.
	 *
	 * @param textEditor the active text editor
	 * @return the start line of the selected text
	 */
	public static int getSelectedStartLine(ITextEditor textEditor) {
		ITextSelection textSelection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
		return textSelection.getStartLine() + 1; // +1 to match the editor being 1-indexed.
	}

	/**
	 * Returns the end line of the selected text in the active text editor.
	 *
	 * @param textEditor the active text editor
	 * @return the end line of the selected text
	 */
	public static int getSelectedEndLine(ITextEditor textEditor) {
		ITextSelection textSelection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
		return textSelection.getEndLine() + 1; // +1 to match the editor being 1-indexed.
	}

	/**
	 * Returns the currently selected project in the workspace.
	 * This can be used as a fallback when no active editor is available.
	 *
	 * @return the selected project or null if no project is selected
	 */
	public static IProject getSelectedProject() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				var selection = page.getSelection();
				if (selection instanceof org.eclipse.jface.viewers.IStructuredSelection) {
					var structuredSelection = (org.eclipse.jface.viewers.IStructuredSelection) selection;
					Object firstElement = structuredSelection.getFirstElement();

					// Handle different types of selected elements
					if (firstElement instanceof IProject) {
						return (IProject) firstElement;
					} else if (firstElement instanceof IResource) {
						return ((IResource) firstElement).getProject();
					} else if (firstElement != null) {
						// Try to adapt to IResource
						IResource resource = Platform.getAdapterManager().getAdapter(firstElement, IResource.class);
						if (resource != null) {
							return resource.getProject();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the text of the active file.
	 *
	 * @param activeFile the active file
	 * @return the text of the active file
	 */
	public static String getEditorText(IFile activeFile) {
		try {
			return new String(Files.readAllBytes(activeFile.getLocation().toFile().toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the text of the active file (used as a fallback for reading non-workspace files).
	 *
	 * @param textEditor the active file
	 * @return the text of the active file
	 */
	public static String getEditorText(ITextEditor textEditor) {
		IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		if (document != null) {
			return document.get();
		} else {
			throw new IllegalStateException("Cannot retrieve document from the editor.");
		}
	}

	/**
	 * Returns the title of the active editor.
	 *
	 * @param textEditor the active file
	 * @return the text of the active file
	 */
	public static String getEditorTitle(ITextEditor textEditor) {
		return textEditor.getTitle();
	}

	/**
	 * Returns the selected text in the active text editor if any, otherwise returns
	 * the text of the active file.
	 *
	 * @param textEditor the active text editor
	 * @return the selected text or the text of the active file
	 */
	public static String getSelectedTextOrEditorText(ITextEditor textEditor) {
		String text = getSelectedText(textEditor);
		if (text.isEmpty()) {
			IFile activeFile = textEditor.getEditorInput().getAdapter(IFile.class);
			text = getEditorText(activeFile);
		}
		return text;
	}

	/**
	 * Returns the name of the current project as a string.
	 * First tries to get the project from the active text editor,
	 * then falls back to the selected project in the workspace.
	 *
	 * @return the current project name or null if no project is found
	 */
	public static String getCurrentProjectName() {
		// First try to get project from active text editor
		ITextEditor activeTextEditor = getActiveTextEditor();
		if (activeTextEditor != null) {
			IProject project = getActiveProject(activeTextEditor);
			if (project != null) {
				return project.getName();
			}
		}

		// Fall back to selected project
		IProject selectedProject = getSelectedProject();
		if (selectedProject != null) {
			return selectedProject.getName();
		}

		return null;
	}

	/**
	 * Returns the contents of the clipboard as a string.
	 *
	 * @return the contents of the clipboard
	 */
	public static String getClipboardContents() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		Object clipboardContents = clipboard.getContents(TextTransfer.getInstance());
		String clipboardText = clipboardContents != null ? clipboardContents.toString() : "";
		clipboard.dispose();
		return clipboardText;
	}

	/**
	 * Returns the compiler warnings for the active file.
	 *
	 * @param activeFile the active file
	 * @return the compiler warnings
	 */
	public static String getCompilerWarnings(IFile activeFile) {
		return getCompilerMessages(activeFile, IMarker.SEVERITY_WARNING);
	}

	/**
	 * Returns the compiler errors for the active file.
	 *
	 * @param activeFile the active file
	 * @return the compiler errors
	 */
	public static String getCompilerErrors(IFile activeFile) {
		return getCompilerMessages(activeFile, IMarker.SEVERITY_ERROR);
	}

	/**
	 * Replaces the selected text in the active text editor with the given text and
	 * selects the new text.
	 *
	 * @param textEditor the active text editor
	 * @param newText    the new text to replace the selected text
	 */
	public static void replaceAndSelectText(ITextEditor textEditor, String newText) {
		IDocumentProvider provider = textEditor.getDocumentProvider();
		IDocument document = provider.getDocument(textEditor.getEditorInput());
		ITextSelection textSelection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
		int offset;
		int length;
		if (!textSelection.getText().isEmpty()) {
			offset = textSelection.getOffset();
			length = textSelection.getLength();
		} else {
			offset = 0;
			length = document.getLength();
		}
		try {
			document.replace(offset, length, newText);
			setSelection(textEditor, document, offset, newText.length());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the selection in the active text editor to the given offset and length.
	 *
	 * @param textEditor the active text editor
	 * @param document   the document of the active text editor
	 * @param offset     the offset of the new selection
	 * @param length     the length of the new selection
	 */
	public static void setSelection(ITextEditor textEditor, IDocument document, int offset, int length) {
		ISelectionProvider selectionProvider = textEditor.getSite().getSelectionProvider();
		ITextSelection newTextSelection = new TextSelection(document, offset, length);
		selectionProvider.setSelection(newTextSelection);
	}

	/**
	 * Sets the contents of the clipboard to the given string.
	 *
	 * @param text the text to set on the clipboard
	 */
	public static void setClipboardContents(String text) {
		Clipboard clipboard = null;
		try {
			clipboard = new Clipboard(getDisplay());
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (clipboard != null && !clipboard.isDisposed()) {
				clipboard.dispose();
			}
		}
	}

	/**
	 * Saves all open editors in the current workbench.
	 *
	 * @param confirm whether to confirm before saving each editor
	 */
	public static void saveAllEditors(boolean confirm) {
		Eclipse.getWorkbench().saveAllEditors(confirm);
	}

	/**
	 * Creates a new button with the given parameters.
	 *
	 * @param parent      The Composite widget that will be the parent of the new
	 *                    button.
	 * @param buttonName  The text displayed on the button.
	 * @param tooltipText The tooltip text displayed when hovering over the button
	 *                    (if tooltips are enabled).
	 * @param filename    The filename of the icon to be set for the button.
	 * @param listener    The SelectionAdapter that will handle button click events.
	 * @return A new Button instance with the specified properties and behavior.
	 */
	public static Button createButton(Composite parent, String buttonName, String tooltipText, String filename,
			SelectionAdapter listener) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(buttonName);
		button.setToolTipText(tooltipText);

		// Set the button's image and ensure it's properly managed
		Image image = loadIcon(filename);
		button.setImage(image);

		// Add a dispose listener to ensure the image is disposed when the button is disposed
		button.addDisposeListener(e -> {
			Image buttonImage = button.getImage();
			if (buttonImage != null && !buttonImage.isDisposed()) {
				buttonImage.dispose();
			}
		});

		button.addSelectionListener(listener);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return button;
	}

	/**
	 * Sets the icon of a Button using an image file located in the ICONS_PATH.
	 *
	 * @param button   The Button to set the image for.
	 * @param filename The name of the image file, including the extension (e.g.,
	 *                 "icon.png").
	 */
	// In Eclipse.java
	public static void setButtonIcon(Button button, String filename) {
		runOnUIThreadSync(() -> {
			// Dispose old image first
			Image oldIcon = button.getImage();
			if (oldIcon != null && !oldIcon.isDisposed()) {
				oldIcon.dispose();
			}

			// Create and set new image
			Image newIcon = loadIcon(filename);
			button.setImage(newIcon);
		});
	}

	/**
	 * Loads an image from the specified file path.
	 *
	 * @param filename The name of the image file, including the extension (e.g., "icon.png").
	 * @return The loaded image.
	 * @throws RuntimeException If there is an error loading the image.
	 */
	public static Image loadIcon(String filename) {
		URL imageUrl;
		try {
			imageUrl = new URL(Constants.ICONS_PATH + filename);
			// Use ImageDescriptor.createFromURL(imageUrl).createImage(true) to track the image
			// The 'true' parameter enables device tracking which helps with disposal
			return ImageDescriptor.createFromURL(imageUrl).createImage(true);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts an image from the specified file path to a base64 string.
	 *
	 * @param filename The name of the image file, including the extension (e.g., "icon.png").
	 * @return The image as a base64 encoded string.
	 * @throws RuntimeException If there is an error loading or encoding the image.
	 */
	public static String loadIconAsBase64(String filename) {
		URL imageUrl;
		try {
			imageUrl = new URL(Constants.ICONS_PATH + filename);
			try (InputStream stream = imageUrl.openStream()) {
				byte[] imageBytes = stream.readAllBytes();
				return Base64.getEncoder().encodeToString(imageBytes);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new GridLayout with the specified parameters.
	 *
	 * @param numColumns        The number of columns in the layout.
	 * @param equalWidth        Whether the columns should have equal width.
	 * @param marginWidth       The width of the margins around the layout. A value
	 *                          of -1 means use the default margin width.
	 * @param marginHeight      The height of the margins around the layout. A value
	 *                          of -1 means use the default margin height.
	 * @param horizontalSpacing The horizontal spacing between the controls in the
	 *                          layout. A value of -1 means use the default
	 *                          horizontal spacing.
	 * @param verticalSpacing   The vertical spacing between the controls in the
	 *                          layout. A value of -1 means use the default vertical
	 *                          spacing.
	 * @return The created GridLayout.
	 */
	public static GridLayout createGridLayout(int numColumns, boolean equalWidth, int marginWidth, int marginHeight,
			int horizontalSpacing, int verticalSpacing) {
		GridLayout layout = new GridLayout(numColumns, equalWidth);
		if (marginWidth != -1) {
			layout.marginWidth = marginWidth;
		}
		if (marginHeight != -1) {
			layout.marginHeight = marginHeight;
		}
		if (horizontalSpacing != -1) {
			layout.horizontalSpacing = horizontalSpacing;
		}
		if (verticalSpacing != -1) {
			layout.verticalSpacing = verticalSpacing;
		}
		return layout;
	}

	/**
	 * Creates a new GridData object with the specified parameters.
	 *
	 * @param grabExcessHorizontalSpace Whether the control should grab excess
	 *                                  horizontal space.
	 * @param grabExcessVerticalSpace   Whether the control should grab excess
	 *                                  vertical space.
	 * @return The created GridData object.
	 */
	public static GridData createGridData(boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace) {
		return new GridData(SWT.FILL, SWT.FILL, grabExcessHorizontalSpace, grabExcessVerticalSpace);
	}

	/**
	 * Executes the given runnable on the UI thread asynchronously.
	 *
	 * @param runnable the runnable to be executed on the UI thread.
	 */
	public static void runOnUIThreadAsync(Runnable runnable) {
		Display.getDefault().asyncExec(runnable);
	}

	/**
	 * Executes the given runnable on the UI thread synchronously.
	 *
	 * @param runnable the runnable to be executed on the UI thread.
	 */
	public static void runOnUIThreadSync(Runnable runnable) {
		Display.getDefault().syncExec(runnable);
	}

	/**
	 * Executes the given supplier on the UI thread synchronously and returns its
	 * result.
	 *
	 * @param supplier the supplier to be executed on the UI thread.
	 * @return the result of the supplier's execution.
	 */
	public static Object runOnUIThreadSync(Supplier<Object> supplier) {
		AtomicReference<Object> returnObject = new AtomicReference<>();
		Display.getDefault().syncExec(() -> {
			returnObject.set(supplier.get());
		});
		return returnObject.get();
	}

	/**
	 * Executes a script in the browser widget asynchronously on the UI thread.
	 *
	 * @param script The script to execute.
	 */
	public static void executeScript(Browser browser, String script) {
		runOnUIThreadAsync(() -> browser.execute(script));
	}

	/**
	 * Evaluates a script in the browser widget synchronously on the UI thread and
	 * returns the result.
	 *
	 * @param script The script to evaluate.
	 * @return The result of the script evaluation.
	 */
	public static Object evaluateScript(Browser browser, String script) {
		return runOnUIThreadSync(() -> browser.evaluate(script));
	}

	/**
	 * Returns the state location for the plugin.
	 *
	 * @param pluginId The ID of the plugin
	 * @return The state location as a File object
	 */
	public static File getPluginStateLocation(String pluginId) {
		IPath statePath = Platform.getStateLocation(Platform.getBundle(pluginId));
		return statePath.toFile();
	}

	/**
	 * Returns the compiler messages for the active file with the given severity.
	 *
	 * @param activeFile the active file
	 * @param severity   the severity of the compiler messages to return
	 * @return the compiler messages
	 */
	private static String getCompilerMessages(IFile activeFile, int severity) {
		StringBuilder compilerMessages = new StringBuilder();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (IProject project : workspaceRoot.getProjects()) {
			try {
				if (project.isOpen()) {
					IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					for (IMarker marker : markers) {
						IResource markerResource = marker.getResource();
						if (markerResource instanceof IFile
								&& ((IFile) markerResource).getFullPath().equals(activeFile.getFullPath())) {
							int markerSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
							if (markerSeverity == severity) {
								String message = marker.getAttribute(IMarker.MESSAGE, "");
								int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
								compilerMessages.append("* \"").append(message).append("\" at line ").append(lineNumber)
								.append(" in file `").append(activeFile.getName()).append("`\n");
							}
						}
					}
				}
			} catch (CoreException e) {
				Logger.error(e.getMessage(), e);
			}
		}
		return compilerMessages.toString().trim();
	}

}