package eclipse.plugin.aiassistant.utility;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eclipse.plugin.aiassistant.Constants;

/**
 * This class provides utility methods for handling file extensions and their corresponding programming languages.
 */
public class LanguageFileExtensions {
	
	/**
	 * A map that stores the mapping between file extensions and their corresponding programming languages.
	 */
	private static Map<String, String> extensionToLanguage;
	
	// Static initialization block to load the JSON file and populate the map.
	static {
		
		// Load the JSON file.
	    ObjectMapper jsonMapper = new ObjectMapper();
	    JsonNode jsonTree;
	    try {
	        jsonTree = jsonMapper.readTree(loadFile(Constants.JSON_PATH, Constants.HIGHLIGHT_JS_LANGUAGES_FILENAMES));
	     } catch (JsonMappingException e) {
	        throw(new RuntimeException(e));
	     } catch (JsonProcessingException e) {
	    	throw(new RuntimeException(e));
	     }

	     // Populate the map.
	    extensionToLanguage = new HashMap<>();
	    for (JsonNode node : jsonTree) {
	        String language = node.get("language").asText();
	        JsonNode extensionsNode = node.get("extensions");
	        if (extensionsNode.isArray()) {
	            for (JsonNode extensionNode : extensionsNode) {
	                String ext = extensionNode.asText();
	                extensionToLanguage.put(ext, language);
	             }
	         }
	     }
	}
	
	/**
	 * Returns the programming language name corresponding to the given filename.
	 * 
	 * @param filename The filename for which the programming language is required.
	 * @return The programming language name if found, else an empty string.
	 */
	public static String getLanguageName(String filename) {
		String extension = getFileExtension(filename);
		return extensionToLanguage.getOrDefault(extension, "");
	}
	
	/**
	 * Returns the markdown tag corresponding to the given filename.
	 * 
	 * @param filename The filename for which the markdown tag is required.
	 * @return The markdown tag if found, else an empty string.
	 */
	public static String getMarkdownTag(String filename) {
		String extension = getFileExtension(filename);
		if (extensionToLanguage.containsKey(extension)) {
			return extension;
		}
		if (extensionToLanguage.containsKey(filename)) {	// eg: "Markdown".
			return filename;
		}
		return "";
	}

	/**
	 * Extracts the file extension from the given filename.
	 * 
	 * @param filename The filename from which the file extension is to be extracted.
	 * @return The file extension if found, else an empty string.
	 */
	private static String getFileExtension(String filename) {
	    int lastDotIndex = filename.lastIndexOf('.');
	    if (lastDotIndex == -1 || lastDotIndex +1 >= filename.length()) {
	        return ""; 	// Last character in the string or no extension.
	     }
	    return filename.substring(lastDotIndex + 1);
	}
	
	/**
	 * Loads the file from the given filepath and filename.
	 * 
	 * @param filepath The path of the file to be loaded.
	 * @param filename The name of the file to be loaded.
	 * @return The content of the file as a string.
	 */
	private static String loadFile(String filepath, String filename) {
		try (InputStream in = FileLocator.toFileURL(new URL(filepath + filename)).openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}