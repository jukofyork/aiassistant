package eclipse.plugin.aiassistant.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.chat.ChatRole;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.prompt.PromptLoader;

/**
 * Client for interacting with OpenAI-compatible APIs to handle chat completions.
 * Supports both streaming and non-streaming responses, with configurable models
 * and parameters. Implements the publisher-subscriber pattern for streaming responses.
 */
public class OpenAiApiClient {
		
	/** Publisher for streaming API responses. Created on subscribe() and cleared after closing. */
	private SubmissionPublisher<String> streamingResponsePublisher = null;

	/** Callback to check if the current operation should be cancelled */
	private Supplier<Boolean> isCancelled = () -> false;

	public OpenAiApiClient() {
	}

    /**
     * Tests API connectivity and retrieves available models.
     *
     * @param apiUrl Base URL of the API endpoint
     * @param apiKey Authentication key for API access
     * @return "OK" if API is accessible, otherwise an error message
     */
	public static String getApiStatus(String apiUrl, String apiKey) {
		try {
        	new HttpClientWrapper(
    				getApiEndpoint(apiUrl, Constants.CHAT_COMPLETION_API_URL).toURI(),
    				apiKey,
        			Duration.ofMillis(1000),
        			Duration.ofMillis(1000));
		} catch (Exception e) {
			return "No OpenAI compatible API found at '" + apiUrl + "'... Check URL and/or Key.";
		}
		return "OK";
	}
	
    /**
     * Retrieves the list of available models from the API.
     *
     * @param baseUrlString Base URL of the API endpoint
     * @param apiKey Authentication key for API access
     * @return List of model names, empty list if request fails
     */
    public static List<String> fetchAvailableModelNames(String baseUrlString, String apiKey) {
        List<String> modelNames = new ArrayList<>();
        try {
        	HttpClientWrapper client = new HttpClientWrapper(
    				getApiEndpoint(baseUrlString, Constants.MODEL_LIST_API_URL).toURI(),
    				apiKey,
        			Duration.ofMillis(1000),
        			Duration.ofMillis(1000));
            HttpResponse<InputStream> response = client.sendRequest( null);
            InputStream responseBody = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("data") && rootNode.get("data").isArray()) {
                for (JsonNode modelNode : rootNode.get("data")) {
                    if (modelNode.has("id")) {
                        modelNames.add(modelNode.get("id").asText());
                    }
                }
            }
            Collections.sort(modelNames);
        } catch (Exception e) {
            modelNames.clear();
        }
        return modelNames;
    }
    
    /**
     * Validates the current model configuration and API connectivity.
     * 
     * @return "OK" if configuration is valid, otherwise returns an error message
     */
	public String checkCurrentModelStatus() {
		if (Preferences.getCurrentModelName().isEmpty()) {
			return "No model selected. Check settings...";
		}
		String status = getApiStatus(Preferences.getCurrentApiUrl(), Preferences.getCurrentApiKey());
		if (!status.equals("OK")) {
			return status;
		}
		List<String> modelNames = fetchAvailableModelNames(Preferences.getCurrentApiUrl(),
				Preferences.getCurrentApiKey());
		if (!modelNames.contains(Preferences.getCurrentModelName())) {
			return "No model named '" + Preferences.getCurrentModelName() + "' available at '"
					+ Preferences.getCurrentApiUrl() + "'. Check settings...";
		}
		return "OK";
	}
    	
	/**
	 * Subscribes a subscriber to receive streaming String data the OpenAI API.
	 * 
	 * @param subscriber The subscriber to be subscribed to the publisher.
	 */
	public synchronized void subscribe(Flow.Subscriber<String> subscriber) {
		if (streamingResponsePublisher == null) {
			streamingResponsePublisher = new SubmissionPublisher<>();
		}
		streamingResponsePublisher.subscribe(subscriber);
	}

	/**
	 * Sets a cancel provider capable of stopping the streaming loop.
	 * 
	 * @param isCancelled A Supplier that returns a Boolean indicating whether the
	 *                    operation should be cancelled.
	 */
	public void setCancelProvider(Supplier<Boolean> isCancelled) {
		this.isCancelled = isCancelled;
	}

    /**
     * Executes a chat completion request with the provided conversation.
     * Handles both streaming and non-streaming responses based on preferences
     * and model capabilities.
     *
     * @param chatConversation The conversation history and current prompt
     * @return A Runnable that executes the API request when run
     */
	public Runnable run(ChatConversation chatConversation) {
		return () -> {
			try {
				String modelStatus = checkCurrentModelStatus();
				if (!modelStatus.equals("OK")) {
					throw new Exception(modelStatus);
				}
				String modelName = Preferences.getCurrentModelName();
				HttpClientWrapper httpClientWrapper = new HttpClientWrapper(
						getApiEndpoint(Preferences.getCurrentApiUrl(), Constants.CHAT_COMPLETION_API_URL).toURI(),
						Preferences.getCurrentApiKey(),
						Duration.ofMillis(Preferences.getConnectionTimeout()),
						Duration.ofMinutes(Constants.DEFAULT_REQUEST_TIMEOUT));
				HttpResponse<InputStream> streamingResponse = httpClientWrapper.sendRequest(
						buildChatCompletionRequestBody(modelName, chatConversation));
				// NOTE: We can't use streaming for "o1-mini" or "o1-preview" models.
				if (!Preferences.useStreaming() || modelName.contains("o1-mini") || modelName.contains("o1-preview")) {
					processResponse(streamingResponse);
				}
				else {
					processStreamingResponse(streamingResponse);
				}
			} catch (Exception e) {
				if (streamingResponsePublisher != null)
					streamingResponsePublisher.closeExceptionally(e);
				streamingResponsePublisher = null;
			} finally {
				if (streamingResponsePublisher != null)
					streamingResponsePublisher.close();
				streamingResponsePublisher = null;
			}
		};
	}
	
    /**
     * Constructs the JSON request body for the chat completion API.
     * Handles special cases for different model types and includes
     * appropriate configuration based on model capabilities.
     *
     * @param modelName The name of the model to use
     * @param chatConversation The conversation to process
     * @return JSON string containing the request body
     * @throws Exception if request body creation fails
     */
	private String buildChatCompletionRequestBody(String modelName, ChatConversation chatConversation) throws Exception {
		try {
			var objectMapper = new ObjectMapper();
			var requestBody = objectMapper.createObjectNode();
			var jsonMessages = objectMapper.createArrayNode();

			// Add the model ID first.
			requestBody.put("model", modelName);

			// Add the message history so far.
			// NOTE: We can't use a system message for "o1-mini" or "o1-preview" models.
			if (!modelName.contains("o1-mini") && !modelName.contains("o1-preview")) {
				var systemMessage = objectMapper.createObjectNode();
				systemMessage.put("role", "system");
				systemMessage.put("content", PromptLoader.getSystemPromptText());
				jsonMessages.add(systemMessage);
			}
			for (ChatMessage message : chatConversation.messages()) {
				if (Objects.nonNull(message.getMessage())) {
					var jsonMessage = objectMapper.createObjectNode();

					// If the last role was the same, concatenate this message's text.
					if (jsonMessages.size() > 0) {
						var lastMessage = jsonMessages.get(jsonMessages.size() - 1);
						if (lastMessage.get("role").asText().equals(message.getRole().getRoleName())) {
							jsonMessage.put("role", message.getRole().getRoleName());
							jsonMessage.put("content",
									lastMessage.get("content").asText() + "\n" + message.getMessage());
							jsonMessages.remove(jsonMessages.size() - 1);
							jsonMessages.add(jsonMessage);
						}
					}

					// If not concatenated above and is user or assistant message, add it.
					if (jsonMessage.isEmpty()
							&& (message.getRole() == ChatRole.USER || message.getRole() == ChatRole.ASSISTANT)) {
						jsonMessage.put("role", message.getRole().getRoleName());
						jsonMessage.put("content", message.getMessage());
						jsonMessages.add(jsonMessage);
					}

				}
			}
			requestBody.set("messages", jsonMessages);

			// Add the temperature to the request.
			// NOTE: We can't set temperature for "o1-mini" or "o1-preview" models.
			if (!modelName.contains("o1-mini") && !modelName.contains("o1-preview")) {
				requestBody.put("temperature", Preferences.getCurrentTemperature());
			}

			// Set the streaming flag.
			// NOTE: We can't use streaming for "o1-mini" or "o1-preview" models.
			if (!Preferences.useStreaming() || modelName.contains("o1-mini") || modelName.contains("o1-preview")) {
				requestBody.put("stream", false);
			} else {
				requestBody.put("stream", true);
				var node = objectMapper.createObjectNode();
				node.put("include_usage", true);
				requestBody.putPOJO("stream_options", node);
			}
			
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
			
		} catch (JsonProcessingException e) {
			throw new Exception("Failed to build chat completione request body", e);
		}
	}

	/**
	 * Processes the (non-streaming) response from the OpenAI API.
	 * 
	 * @param response The HTTP response from the OpenAI API.
	 * @throws IOException If an error occurs while processing the response.
	 */
	private void processResponse(HttpResponse<InputStream> response) throws IOException {
	    String modelName = "";
	    String finishReason = "";
	    String usageStatistics = "";
	
	    try (var inputStream = response.body();
	         var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
	         var bufferedReader = new BufferedReader(reader)) {
	
	        // Read the entire response into a single string
	        String result = bufferedReader.lines().collect(Collectors.joining("\n"));
	        //Logger.info(result);
	
	        // Parse the JSON from the complete response
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode jsonTree = mapper.readTree(result);
	
	        // Extract data from the JSON tree
	        if (jsonTree.has("model")) {
	            modelName = "Model: " + jsonTree.get("model").asText();
	        }
	
	        if (jsonTree.has("choices") && jsonTree.get("choices").has(0)) {
	            JsonNode choiceNode = jsonTree.get("choices").get(0);
	            if (choiceNode.has("finish_reason")) {
	                finishReason = "Finish: " + choiceNode.get("finish_reason").asText();
	            }
	            if (choiceNode.has("message") && choiceNode.get("message").has("content")) {
	            	String responseContent = choiceNode.get("message").get("content").asText();
	                streamingResponsePublisher.submit(responseContent.toString());
	            }
	        }
	
	        if (jsonTree.has("usage")) {
	            JsonNode usageNode = jsonTree.get("usage");
	            usageStatistics = generateUsageStatistics(usageNode);
	        }
	
	    } catch (IOException e) {
	        throw new IOException("Failed to process the response", e);
	    }
	
	    // Log the extracted information
	    if (isCancelled.get()) {
	    	streamingResponsePublisher.closeExceptionally(new CancellationException());
	    } else {
	    	Logger.info(modelName+"\n"+finishReason+"\n"+usageStatistics);
	    }
	}
	
	/**
	 * Processes the streaming response from the OpenAI API.
	 * 
	 * @param response The HTTP response from the OpenAI API.
	 * @throws IOException If an error occurs while processing the response.
	 */
	private void processStreamingResponse(HttpResponse<InputStream> response) throws IOException {

		String modelName = "";
		String finishReason = "";
		String usageStatistics = "";

		try (var inputStream = response.body();
				var streamingInputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				var streamingResponseReader = new BufferedReader(streamingInputStreamReader)) {

			// These are used so we don't overload the publisher with too many calls.
			StringBuilder responseContentBuffer = new StringBuilder();
			
			// Read each streamed packet as we get them from OpenAI.
			String line;
			while ((line = streamingResponseReader.readLine()) != null && !isCancelled.get()) {

				//Logger.info(line);

				if (line.startsWith("data:")) {
					
					line = line.substring("data:".length()).trim();
					
					// If not done, get the streamed content and append to the buffer.
					if (!line.equals("[DONE]")) {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode jsonTree = mapper.readTree(line);
						
						// Get the model name.
						if (modelName.isEmpty() && jsonTree.has("model")) {
							modelName = "Model: "+jsonTree.get("model").asText();
						}
	
						// Get the finish reason and/or content from the "choices" sub-tree.
						if (jsonTree.has("choices") && jsonTree.get("choices").has(0)) {
							var choiceNode = jsonTree.get("choices").get(0);
							if (choiceNode.has("finish_reason") && !choiceNode.get("finish_reason").asText().equals("null")) {
								finishReason = "Finish : "+choiceNode.get("finish_reason").asText();
							}
							if (choiceNode.has("message") && choiceNode.get("message").has("content")) {
								var messageContent = choiceNode.get("message").get("content").asText();
								if (!messageContent.equals("null")) {
									responseContentBuffer.append(messageContent);
								}
							}
							else if (choiceNode.has("delta") && choiceNode.get("delta").has("content")) {
								var messageContent = choiceNode.get("delta").get("content").asText();
								if (!messageContent.equals("null")) {
									responseContentBuffer.append(messageContent);
								}
							}
						}
						
						// Get the usage stats.
						if (jsonTree.has("usage")) {
							var usageNode = jsonTree.get("usage");
							usageStatistics = generateUsageStatistics(usageNode);
						}
												
					}
					
					// If the queue is empty then deal with the buffered string.
					if (streamingResponsePublisher.estimateMaximumLag() == 0) {
						streamingResponsePublisher.submit(responseContentBuffer.toString());
						responseContentBuffer.setLength(0);
					}
					
				}

			}
			
			// Flush anything left in the buffer.
			streamingResponsePublisher.submit(responseContentBuffer.toString());

		}
		if (isCancelled.get()) {
			streamingResponsePublisher.closeExceptionally(new CancellationException());
		}
		else {
			Logger.info(modelName+"\n"+finishReason+"\n"+usageStatistics);
		}
	}

	/**
	 * Generates response statistics from a JSON tree.
	 * 
	 * @param jsonTree The JSON tree to generate the statistics from.
	 * @return The response statistics as a string.
	 */
	private String generateUsageStatistics(JsonNode usageNode) {
		StringBuilder responseStatistics = new StringBuilder();
		if (verifyAllRequiredFieldsExist(usageNode)) {
			long promptTokens = usageNode.get("prompt_tokens").asLong();
			long completionTokens = usageNode.get("completion_tokens").asLong();
			long totalTokens = usageNode.get("total_tokens").asLong();
			responseStatistics
					.append("Prompt : ")
					.append(promptTokens)
					.append(" tokens\n")					
					.append("Response : ")
					.append(completionTokens)
					.append(" tokens\n")
					.append("Total : ")
					.append(totalTokens)
					.append(" tokens\n");
		}
		return responseStatistics.toString();
	}

	/**
	 * Verifies that all required fields exist in a JSON tree.
	 * 
	 * @param jsonTree The JSON tree to verify.
	 * @return True if all required fields exist, false otherwise.
	 */
	private boolean verifyAllRequiredFieldsExist(JsonNode usageNode) {
		return usageNode.has("completion_tokens") && usageNode.has("prompt_tokens") && usageNode.has("total_tokens");
	}

    /**
     * Constructs the complete API endpoint URL by combining base URL and path.
     *
     * @param apiUrl Base URL of the API
     * @param path API endpoint path to append
     * @return Complete URL for the API endpoint
     * @throws RuntimeException if URL is malformed
     */
	private static URL getApiEndpoint(String apiUrl, String path) {
		try {
			URL baseUrl = new URL(apiUrl);
			return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), baseUrl.getFile() + path);
		} catch (MalformedURLException e) {
			Logger.error("Invalid API base URL", e);
			throw new RuntimeException("Invalid API base URL", e);
		}
	}

}
