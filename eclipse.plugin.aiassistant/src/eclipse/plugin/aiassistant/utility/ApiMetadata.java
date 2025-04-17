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
 * This class provides access to API model data stored in a JSON file.
 * It offers methods to retrieve various properties of AI models based on their name and provider.
 */
public class ApiMetadata {
    private static Map<String, JsonNode> modelData;
    
    static {
        // Load and parse the JSON file containing model data
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode jsonTree;
        try {
            jsonTree = jsonMapper.readTree(loadFile(Constants.JSON_PATH, Constants.API_MODEL_DATA));
        } catch (JsonMappingException e) {
            throw new RuntimeException("Error mapping JSON data", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON data", e);
        }
         
        // Populate the modelData map with parsed JSON data
        modelData = new HashMap<>();
        jsonTree.fields().forEachRemaining(entry -> 
            modelData.put(entry.getKey(), entry.getValue()));
    }
    
    /**
     * Retrieves the maximum number of tokens for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The maximum number of tokens, or 0 if not specified.
     */
    public static int getMaxTokens(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("max_tokens") ? 
            node.get("max_tokens").asInt() : 0;
    }
    
    /**
     * Retrieves the maximum number of input tokens for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The maximum number of input tokens, or 0 if not specified.
     */
    public static int getMaxInputTokens(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("max_input_tokens") ? 
            node.get("max_input_tokens").asInt() : 0;
    }
    
    /**
     * Retrieves the maximum number of output tokens for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The maximum number of output tokens, or 0 if not specified.
     */
    public static int getMaxOutputTokens(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("max_output_tokens") ? 
            node.get("max_output_tokens").asInt() : 0;
    }
    
    /**
     * Retrieves the input cost per token for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The input cost per token, or 0.0 if not specified.
     */
    public static double getInputCostPerToken(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("input_cost_per_token") ? 
            node.get("input_cost_per_token").asDouble() : 0.0;
    }
    
    /**
     * Retrieves the output cost per token for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The output cost per token, or 0.0 if not specified.
     */
    public static double getOutputCostPerToken(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("output_cost_per_token") ? 
            node.get("output_cost_per_token").asDouble() : 0.0;
    }
    
    /**
     * Retrieves the LiteLLM provider for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The LiteLLM provider, or an empty string if not specified.
     */
    public static String getLiteLLMProvider(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("litellm_provider") ? 
            node.get("litellm_provider").asText() : "";
    }
    
    /**
     * Retrieves the mode for a given model and provider.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The mode, or an empty string if not specified.
     */
    public static String getMode(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("mode") ? 
            node.get("mode").asText() : "";
    }
    
    /**
     * Checks if a given model and provider support function calling.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if function calling is supported, false otherwise.
     */
    public static boolean supportsFunctionCalling(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_function_calling") && 
            node.get("supports_function_calling").asBoolean();
    }
    
    /**
     * Checks if a given model and provider support parallel function calling.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if parallel function calling is supported, false otherwise.
     */
    public static boolean supportsParallelFunctionCalling(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_parallel_function_calling") && 
            node.get("supports_parallel_function_calling").asBoolean();
    }
    
    /**
     * Checks if a given model and provider support vision capabilities.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if vision is supported, false otherwise.
     */
    public static boolean supportsVision(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_vision") && 
            node.get("supports_vision").asBoolean();
    }
    
    /**
     * Checks if a given model and provider support audio input.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if audio input is supported, false otherwise.
     */
    public static boolean supportsAudioInput(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_audio_input") && 
            node.get("supports_audio_input").asBoolean();
    }
    
    /**
     * Checks if a given model and provider support audio output.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if audio output is supported, false otherwise.
     */
    public static boolean supportsAudioOutput(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_audio_output") && 
            node.get("supports_audio_output").asBoolean();
    }
    
    /**
     * Checks if a given model and provider support prompt caching.
     *
     * @param modelName The name of the AI model.
     * @param provider The provider of the AI model.
     * @return true if prompt caching is supported, false otherwise.
     */
    public static boolean supportsPromptCaching(String modelName, String provider) {
        JsonNode node = findModelData(modelName, provider);
        return node != null && node.has("supports_prompt_caching") && 
            node.get("supports_prompt_caching").asBoolean();
    }
    
    /**
     * Finds the model data for a given model name and provider.
     * If an exact match is not found, it attempts to find the best partial match.
     *
     * @param model The name of the AI model.
     * @param provider The provider of the AI model.
     * @return The JsonNode containing the model data, or null if not found.
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
    
    /**
     * Loads the file from the given filepath and filename.
     * 
     * @param filepath The path of the file to be loaded.
     * @param filename The name of the file to be loaded.
     * @return The content of the file as a string.
     * @throws RuntimeException if an IOException occurs while reading the file.
     */
    private static String loadFile(String filepath, String filename) {
        try (InputStream in = FileLocator.toFileURL(new URL(filepath + filename)).openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading file: " + filepath + filename, e);
        }
    }
}