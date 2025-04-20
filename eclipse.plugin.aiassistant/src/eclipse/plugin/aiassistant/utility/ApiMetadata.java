package eclipse.plugin.aiassistant.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eclipse.plugin.aiassistant.Activator;
import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;

/**
 * Provides access to AI model metadata stored in a JSON configuration file.
 *
 * This class manages the retrieval and caching of AI model capabilities, limitations,
 * and pricing information. It handles local storage of the metadata file with automatic
 * updates from a remote source when available. The metadata includes information such as
 * token limits, costs, and supported features for various AI models across different providers.
 *
 * The class is initialized statically and will throw a RuntimeException if the model
 * data cannot be loaded, as this data is essential for the plugin's operation.
 *
 * @see Constants#API_MODEL_DATA_FILENAME
 * @see Constants#API_MODEL_DATA_REMOTE_URL
 */
public class ApiMetadata {

	public static final int CONNECTION_TIMEOUT_MS = 1000;
	public static final int REQUEST_TIMEOUT_MS = 5000;

	private static Map<String, JsonNode> modelData;

	/**
	 * Static initializer that loads model data from the JSON configuration file.
	 *
	 * This block executes when the class is first loaded. It:
	 * 1. Updates the local copy of the model data file if a newer version is available
	 * 2. Parses the JSON content into a structured format
	 * 3. Populates the modelData map for quick access
	 *
	 * Any exceptions during initialization are caught and wrapped in RuntimeExceptions,
	 * which will cause the class loading to fail if the model data cannot be loaded.
	 */
	static {
		try {
			// First ensure we have an up-to-date local copy and then load it
			String jsonContent = updateLocalCopyAndGetContent();

			// Parse the JSON content
			ObjectMapper jsonMapper = new ObjectMapper();
			JsonNode jsonTree = jsonMapper.readTree(jsonContent);

			// Populate the modelData map with parsed JSON data
			modelData = new HashMap<>();
			jsonTree.fields().forEachRemaining(entry ->
			modelData.put(entry.getKey(), entry.getValue()));
		} catch (JsonMappingException e) {
			throw new RuntimeException("Error mapping JSON data", e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error processing JSON data", e);
		} catch (IOException e) {
			throw new RuntimeException("Error accessing model data file", e);
		}
	}

	/**
	 * Retrieves the maximum number of tokens for a given model and provider.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The maximum number of tokens, or 0 if not specified
	 */
	public static int getMaxTokens(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("max_tokens") ?
				node.get("max_tokens").asInt() : 0;
	}

	/**
	 * Retrieves the maximum number of input tokens for a given model and provider.
	 * This represents the maximum context length the model can process.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The maximum number of input tokens, or 0 if not specified
	 */
	public static int getMaxInputTokens(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("max_input_tokens") ?
				node.get("max_input_tokens").asInt() : 0;
	}

	/**
	 * Retrieves the maximum number of output tokens for a given model and provider.
	 * This represents the maximum length of response the model can generate.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The maximum number of output tokens, or 0 if not specified
	 */
	public static int getMaxOutputTokens(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("max_output_tokens") ?
				node.get("max_output_tokens").asInt() : 0;
	}

	/**
	 * Retrieves the input cost per token for a given model and provider.
	 * This is used for cost estimation and billing calculations.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The input cost per token in USD, or 0.0 if not specified
	 */
	public static double getInputCostPerToken(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("input_cost_per_token") ?
				node.get("input_cost_per_token").asDouble() : 0.0;
	}

	/**
	 * Retrieves the output cost per token for a given model and provider.
	 * This is used for cost estimation and billing calculations.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The output cost per token in USD, or 0.0 if not specified
	 */
	public static double getOutputCostPerToken(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("output_cost_per_token") ?
				node.get("output_cost_per_token").asDouble() : 0.0;
	}

	/**
	 * Retrieves the LiteLLM provider identifier for a given model and provider.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The LiteLLM provider identifier, or an empty string if not specified
	 */
	public static String getLiteLLMProvider(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("litellm_provider") ?
				node.get("litellm_provider").asText() : "";
	}

	/**
	 * Retrieves the operational mode for a given model and provider.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return The mode identifier, or an empty string if not specified
	 */
	public static String getMode(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("mode") ?
				node.get("mode").asText() : "";
	}

	/**
	 * Checks if a given model and provider support function calling capabilities.
	 * Function calling allows the model to request execution of specific functions.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if function calling is supported, false otherwise
	 */
	public static boolean supportsFunctionCalling(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_function_calling") &&
				node.get("supports_function_calling").asBoolean();
	}

	/**
	 * Checks if a given model and provider support parallel function calling.
	 * Parallel function calling allows the model to request multiple functions simultaneously.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if parallel function calling is supported, false otherwise
	 */
	public static boolean supportsParallelFunctionCalling(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_parallel_function_calling") &&
				node.get("supports_parallel_function_calling").asBoolean();
	}

	/**
	 * Checks if a given model and provider support vision capabilities.
	 * Vision capabilities allow the model to process and understand image inputs.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if vision is supported, false otherwise
	 */
	public static boolean supportsVision(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_vision") &&
				node.get("supports_vision").asBoolean();
	}

	/**
	 * Checks if a given model and provider support audio input processing.
	 * This capability allows the model to accept and process audio files.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if audio input is supported, false otherwise
	 */
	public static boolean supportsAudioInput(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_audio_input") &&
				node.get("supports_audio_input").asBoolean();
	}

	/**
	 * Checks if a given model and provider support audio output generation.
	 * This capability allows the model to generate spoken responses.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if audio output is supported, false otherwise
	 */
	public static boolean supportsAudioOutput(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_audio_output") &&
				node.get("supports_audio_output").asBoolean();
	}

	/**
	 * Checks if a given model and provider support prompt caching.
	 * Prompt caching can improve performance by reusing results for identical inputs.
	 *
	 * @param modelName The name of the AI model
	 * @param provider The provider of the AI model
	 * @return true if prompt caching is supported, false otherwise
	 */
	public static boolean supportsPromptCaching(String modelName, String provider) {
		JsonNode node = findModelData(modelName, provider);
		return node != null && node.has("supports_prompt_caching") &&
				node.get("supports_prompt_caching").asBoolean();
	}

	/**
	 * Ensures we have an up-to-date local copy of the model data file and returns its content.
	 *
	 * This method implements a smart update strategy:
	 * 1. Checks if a local copy exists in the workspace state location
	 * 2. Creates it from the plugin bundle if needed
	 * 3. Attempts to update it from the remote source if available and:
	 *    - The remote file size differs from the local file size, or
	 *    - The remote file has a newer timestamp
	 * 4. Reads and returns the content of the local file
	 *
	 * If the remote update fails for any reason (network issues, etc.), the method
	 * will fall back to using the existing local copy and log the error.
	 *
	 * @return The content of the model data file as a string
	 * @throws IOException if file operations fail when creating or reading the local copy
	 */
	private static String updateLocalCopyAndGetContent() throws IOException {
		// Get plugin state location for storing updatable files
		File stateDir = Eclipse.getPluginStateLocation(Activator.PLUGIN_ID);
		if (!stateDir.exists()) {
			stateDir.mkdirs();
		}

		File localModelFile = new File(stateDir, Constants.API_MODEL_DATA_FILENAME);

		// If local file doesn't exist, copy from plugin bundle
		if (!localModelFile.exists()) {
			try (InputStream in = FileLocator
					.toFileURL(new URL(Constants.JSON_PATH + Constants.API_MODEL_DATA_FILENAME)).openStream();
					FileOutputStream out = new FileOutputStream(localModelFile)) {
				in.transferTo(out);
			}
			Logger.info("Created local copy of '" + Constants.API_MODEL_DATA_FILENAME + "' from plugin bundle");
		}

		// Try to update from remote if available
		try {
			URL remoteUrl = new URL(Constants.API_MODEL_DATA_REMOTE_URL + Constants.API_MODEL_DATA_FILENAME);
			URLConnection connection = remoteUrl.openConnection();
			connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
			connection.setReadTimeout(REQUEST_TIMEOUT_MS);

			// Get remote file size
			int remoteFileSize = connection.getContentLength();
			long localFileSize = localModelFile.length();

			boolean needsUpdate = false;

			// Check if sizes differ
			if (remoteFileSize > 0 && remoteFileSize != localFileSize) {
				needsUpdate = true;
				Logger.info("Remote file size (" + remoteFileSize + " bytes) differs from local file size ("
						+ localFileSize + " bytes). Updating...");
			} else {
				// If sizes are the same, check modification time as a secondary check
				long remoteTimestamp = connection.getLastModified();
				long localTimestamp = localModelFile.lastModified();

				if (remoteTimestamp > 0 && remoteTimestamp > localTimestamp) {
					needsUpdate = true;
					Logger.info("Remote file is newer. Updating...");
				}
			}

			if (needsUpdate) {
				try (InputStream in = connection.getInputStream();
						FileOutputStream out = new FileOutputStream(localModelFile)) {
					in.transferTo(out);
					Logger.info("Successfully updated '" + Constants.API_MODEL_DATA_FILENAME + "' from remote source");
				}
			}
		} catch (IOException e) {
			Logger.warning("Could not update model data from remote source: " + e.getMessage(), e);
		}

		// Read and return the content of the local file
		return Files.readString(localModelFile.toPath(), StandardCharsets.UTF_8);
	}

	/**
	 * Finds the model data for a given model name and provider.
	 *
	 * This method implements a fallback strategy:
	 * 1. First attempts to find an exact match for the model name
	 * 2. If not found, looks for partial matches where the model name is contained in a key
	 * 3. When multiple partial matches exist, selects the shortest one as the most specific match
	 *
	 * The provider parameter is used to filter matches to ensure the correct provider's
	 * data is returned when multiple providers offer the same model.
	 *
	 * @param model The name of the AI model to find
	 * @param provider The provider of the AI model (can be null or empty)
	 * @return The JsonNode containing the model data, or null if no match is found
	 */
	private static JsonNode findModelData(String model, String provider) {
		if (modelData.containsKey(model)) {
			JsonNode node = modelData.get(model);
			if (provider == null || provider.isEmpty() ||
					(node.has("litellm_provider") && node.get("litellm_provider").asText().equals(provider))) {
				return node;
			}
		}

		JsonNode bestMatch = null;
		int bestMatchLength = Integer.MAX_VALUE;

		for (String key : modelData.keySet()) {
			JsonNode node = modelData.get(key);
			if (key.contains(model)) {
				// Skip if provider doesn't match
				if (provider != null && !provider.isEmpty() &&
						(!node.has("litellm_provider") || !node.get("litellm_provider").asText().equals(provider))) {
					continue;
				}
				// If this match is shorter than our current best match, use it
				if (key.length() < bestMatchLength) {
					bestMatch = node;
					bestMatchLength = key.length();
				}
			}
		}

		return bestMatch;
	}
}