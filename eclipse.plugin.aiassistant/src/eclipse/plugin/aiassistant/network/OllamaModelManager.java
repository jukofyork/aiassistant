package eclipse.plugin.aiassistant.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eclipse.plugin.aiassistant.preferences.Preferences;

/**
 * This class manages the Ollama models. It provides methods to fetch available
 * model names, check if a server is running and if a model is available, and to
 * get the current server status.
 */
public class OllamaModelManager {

	private final HttpClientWrapper httpClientHelper;

	/**
	 * Constructs a new OllamaModelManager.
	 */
	public OllamaModelManager() {
		this.httpClientHelper = new HttpClientWrapper();
	}

	/**
	 * Returns the current server status. If there is no server running or no model
	 * selected, it returns an error message. Otherwise, it returns "OK".
	 *
	 * @return The current server status.
	 */
	public String getCurrentServerStatus() {
		if (!isServerRunning()) {
			return "No Ollama server found running at '" + Preferences.getApiBaseUrl().toString()
					+ "'. Check settings...";
		}
		String modelName = Preferences.getLastSelectedModelName();
		if (modelName.isEmpty()) {
			return "No model selected. Check settings...";
		}
		if (!isModelAvailable(modelName)) {
			return "No model called '" + modelName + "' available in the Ollama server. Check settings...";
		}
		return "OK";
	}

	/**
	 * Fetches the names of all available models from the Ollama server.
	 *
	 * @return An array of model names.
	 */
	public String[] fetchAvailableModelNames() {
		try {
			HttpResponse<InputStream> httpResponse = httpClientHelper
					.sendRequest(Preferences.getModelListApiEndpoint().toURI(), null);
			return extractModelNamesFromResponse(httpResponse);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the name of the last selected model. If the model is not available,
	 * it returns an empty string.
	 *
	 * @return The name of the last selected model.
	 */
	public String getLastSelectedModelName() {
		String modelName = Preferences.getLastSelectedModelName();
		if (isModelAvailable(modelName)) {
			return modelName;
		}
		return "";
	}

	/**
	 * Attempts to load the last selected model into memory. If the model is not
	 * available, it does nothing.
	 */
	public void attemptLoadLastSelectedModelIntoMemory() {
		if (isModelAvailable(getLastSelectedModelName())) {
			try {
				httpClientHelper.sendRequestAsync(Preferences.getCompletionApiEndpoint().toURI(),
						"{ \"model\": \"" + getLastSelectedModelName() + "\"}");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Checks if the Ollama server is running.
	 * 
	 * @return true if the server is reachable, false otherwise.
	 */
	private Boolean isServerRunning() {
		return httpClientHelper.isAddressReachable(URI.create(Preferences.getApiBaseUrl().toString()));
	}

	/**
	 * Checks if a model with the given name is available in the Ollama server.
	 * 
	 * @param modelName The name of the model to check.
	 * @return true if the model is available, false otherwise.
	 */
	private Boolean isModelAvailable(String modelName) {
		String[] availableModels = fetchAvailableModelNames();
		for (String availableModel : availableModels) {
			if (availableModel.equals(modelName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Extracts the names of all models from a HTTP response.
	 * 
	 * @param response The HTTP response to extract model names from.
	 * @return An array of model names.
	 * @throws IOException If an I/O error occurs while reading the response body.
	 */
	private String[] extractModelNamesFromResponse(HttpResponse<InputStream> response) throws IOException {
		try (var responseBodyInputStream = response.body()) {
			ObjectMapper jsonMapper = new ObjectMapper();
			JsonNode jsonTree = jsonMapper.readTree(responseBodyInputStream);
			List<String> modelNames = new ArrayList<>();
			if (jsonTree.has("models")) {
				JsonNode currentModelsArray = jsonTree.get("models");
				if (currentModelsArray.isArray()) {
					for (JsonNode modelNode : currentModelsArray) {
						if (modelNode.has("name")) {
							String currentModelName = modelNode.get("name").asText();
							modelNames.add(currentModelName);
						}
					}
				}
			}
			return modelNames.toArray(String[]::new);
		}
	}

}
