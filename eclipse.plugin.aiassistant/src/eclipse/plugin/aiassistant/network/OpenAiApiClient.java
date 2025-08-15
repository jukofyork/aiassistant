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
import eclipse.plugin.aiassistant.utility.ApiMetadata;

/**
 * Client for interacting with OpenAI-compatible APIs to handle chat completions.
 * Supports both streaming and non-streaming responses, with configurable models
 * and parameters. Implements the publisher-subscriber pattern for streaming responses.
 * Utilizes ApiModelData to retrieve model-specific information for usage reporting.
 */
public class OpenAiApiClient {

	/** Publisher for streaming API responses. Created on subscribe() and cleared after closing. */
	private SubmissionPublisher<String> streamingResponsePublisher = null;

	/** Callback to check if the current operation should be cancelled */
	private Supplier<Boolean> isCancelled = () -> false;

	/** Used to avoid stalling the GUI by having a minimum time between updates */
	private long nextScheduledUpdateTime = 0;

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
					Duration.ofSeconds(Preferences.getConnectionTimeout()),
					Duration.ofSeconds(Preferences.getRequestTimeout()));
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
					Duration.ofSeconds(Preferences.getConnectionTimeout()),
					Duration.ofSeconds(Preferences.getRequestTimeout()));
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
						Duration.ofSeconds(Preferences.getConnectionTimeout()),
						Duration.ofSeconds(Preferences.getRequestTimeout()));
				HttpResponse<InputStream> streamingResponse = httpClientWrapper.sendRequest(
						buildChatCompletionRequestBody(modelName, chatConversation));
				if (Preferences.getCurrentUseStreaming()) {
					processStreamingResponse(streamingResponse);
				}
				else {
					processResponse(streamingResponse);
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

			// Add the system (or developer message for OpenAI reasoning models).
			// NOTE: OpenAI's legacy reasoning models can't use a system or developer message.
			if (Preferences.getCurrentUseSystemMessage() && !isLegacyOpenAiReasoningModel(modelName)) {
				var systemMessage = objectMapper.createObjectNode();

				// "Starting with o1-2024-12-17, o1 models support developer messages rather than system messages"
				if (!isOpenAiReasoningModel(modelName)) {
					systemMessage.put("role", "system");
					systemMessage.put("content", PromptLoader.getSystemPromptText());
				} else {
					systemMessage.put("role", "developer");
					systemMessage.put("content", PromptLoader.getDeveloperPromptText());
				}
				jsonMessages.add(systemMessage);
			}

			// Add the message history so far.
			for (ChatMessage message : chatConversation.messages()) {
				if (Objects.nonNull(message.getMessage())) {
					var jsonMessage = objectMapper.createObjectNode();
					String messageContent = message.getMessage();

					// Process assistant messages to remove <think> tags and any surrounding whitespace that remains
					if (message.getRole() == ChatRole.ASSISTANT) {
						messageContent = messageContent.replaceAll("(?s)<think>.*?</think>\\s*", "");
					}

					// If the last role was the same, concatenate this message's text.
					if (jsonMessages.size() > 0) {
						var lastMessage = jsonMessages.get(jsonMessages.size() - 1);
						if (lastMessage.get("role").asText().equals(message.getRole().getRoleName())) {
							jsonMessage.put("role", message.getRole().getRoleName());
							jsonMessage.put("content",
									lastMessage.get("content").asText() + "\n" + messageContent);
							jsonMessages.remove(jsonMessages.size() - 1);
							jsonMessages.add(jsonMessage);
						}
					}

					// If not concatenated above and is user or assistant message, add it.
					if (jsonMessage.isEmpty()
							&& (message.getRole() == ChatRole.USER || message.getRole() == ChatRole.ASSISTANT)) {
						jsonMessage.put("role", message.getRole().getRoleName());
						jsonMessage.put("content", messageContent);
						jsonMessages.add(jsonMessage);
					}
				}
			}
			requestBody.set("messages", jsonMessages);

			// Add the temperature to the request.
			// NOTE: We can't set a temperature for OpenAI's reasoning models.
			if (!isOpenAiReasoningModel(modelName)) {
				requestBody.put("temperature", Preferences.getCurrentTemperature());
			}

			// Always use "high" reasoning effort for OpenAI's non-legacy reasoning models.
			if (isOpenAiReasoningModel(modelName) && !isLegacyOpenAiReasoningModel(modelName)) {
				requestBody.put("reasoning_effort", "high");
			}

			// Set the streaming flag.
			if (Preferences.getCurrentUseStreaming()) {
				requestBody.put("stream", true);
				var node = objectMapper.createObjectNode();
				node.put("include_usage", true);
				requestBody.putPOJO("stream_options", node);
			}

			//Logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));
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
		String usageReport = "";

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
				if (choiceNode.has("message")) {
					JsonNode messageNode = choiceNode.get("message");
					StringBuilder responseContent = new StringBuilder();

					// Handle DeepSeek API reasoning content
					// SEE: https://api-docs.deepseek.com/guides/reasoning_model
					String deepSeekReasoning = extractFieldText(messageNode, "reasoning_content");
					if (deepSeekReasoning != null) {
						responseContent.append("<think>\n").append(deepSeekReasoning).append("\n</think>\n\n");
					}

					// Handle OpenRouter reasoning content
					// SEE: https://openrouter.ai/docs/use-cases/reasoning-tokens
					String openRouterReasoning = extractFieldText(messageNode, "reasoning");
					if (openRouterReasoning != null) {
						responseContent.append("<think>\n").append(openRouterReasoning).append("\n</think>\n\n");
					}

					// Handle normal/non-reasoning content
					String normalContent = extractFieldText(messageNode, "content");
					if (normalContent != null) {
						responseContent.append(normalContent);
					}

					streamingResponsePublisher.submit(responseContent.toString());
				}
			}

			if (jsonTree.has("usage")) {
				JsonNode usageNode = jsonTree.get("usage");
				usageReport = generateUsageReport(modelName, finishReason, usageNode);
			}

		} catch (IOException e) {
			throw new IOException("Failed to process the response", e);
		}

		// Log the extracted information
		if (isCancelled.get()) {
			streamingResponsePublisher.closeExceptionally(new CancellationException());
		} else {
			Logger.info(usageReport);
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
		String usageReport = "";

		// Used to wrap the thinking block between `<think>` and `</think>` for reasoning APIs.
		boolean isInThinkingBlock = false;

		try (var inputStream = response.body();
				var streamingInputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				var streamingResponseReader = new BufferedReader(streamingInputStreamReader)) {

			// These are used so we don't overload the publisher with too many calls.
			StringBuilder responseContentBuffer = new StringBuilder();

			// Read each streamed packet as we get them from the API.
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
							if (choiceNode.has("message") || choiceNode.has("delta")) {
								JsonNode contentNode = choiceNode.has("message")
										? choiceNode.get("message") : choiceNode.get("delta");

								// Handle DeepSeek API reasoning content
								// SEE: https://api-docs.deepseek.com/guides/reasoning_model
								String deepSeekReasoning = extractFieldText(contentNode, "reasoning_content");
								if (deepSeekReasoning != null) {
									if (!isInThinkingBlock) {
										responseContentBuffer.append("<think>\n");
										isInThinkingBlock = true;
									}
									responseContentBuffer.append(deepSeekReasoning);
								}

								// Handle OpenRouter reasoning content
								// SEE: https://openrouter.ai/docs/use-cases/reasoning-tokens
								String openRouterReasoning = extractFieldText(contentNode, "reasoning");
								if (openRouterReasoning != null) {
									if (!isInThinkingBlock) {
										responseContentBuffer.append("<think>\n");
										isInThinkingBlock = true;
									}
									responseContentBuffer.append(openRouterReasoning);
								}

								// Handle normal/non-reasoning content
								String normalContent = extractFieldText(contentNode, "content");
								if (normalContent != null) {
									if (isInThinkingBlock) {
										responseContentBuffer.append("\n</think>\n\n");
										isInThinkingBlock = false;
									}
									responseContentBuffer.append(normalContent);
								}
							}
						}

						// Get the usage stats.
						if (jsonTree.has("usage")) {
							var usageNode = jsonTree.get("usage");
							usageReport = generateUsageReport(modelName, finishReason, usageNode);
						}

					}

					// If the queue is empty then deal with the buffered string.
					// NOTE: Now also avoids stalling the GUI by having a minimum time between updates.
					if (streamingResponsePublisher.estimateMaximumLag() == 0) {
						long currentTime = System.currentTimeMillis();
						if (nextScheduledUpdateTime == 0) {
							nextScheduledUpdateTime = currentTime + Preferences.getStreamingUpdateInterval();
						}
						else if (currentTime >= nextScheduledUpdateTime) {
							streamingResponsePublisher.submit(responseContentBuffer.toString());
							responseContentBuffer.setLength(0);
							nextScheduledUpdateTime = 0;
						}
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
			Logger.info(usageReport);
		}
	}

	/**
	 * Safely extracts text content from a JSON field if it exists and is not null.
	 *
	 * @param node The JSON node to extract from
	 * @param fieldName The field name to extract
	 * @return The text content, or null if field doesn't exist or is null
	 */
	private String extractFieldText(JsonNode node, String fieldName) {
		if (node.has(fieldName)) {
			JsonNode fieldNode = node.get(fieldName);
			if (!fieldNode.isNull()) {  // NOTE: Check for JSON null so string "null" will be preserved!
				return fieldNode.asText();
			}
		}
		return null;
	}

	/**
	 * Generates a detailed usage report based on the API response and model data.
	 *
	 * @param modelName The name of the model used for the request.
	 * @param finishReason The reason for finishing the request.
	 * @param usageNode The JSON node containing usage information.
	 * @return A formatted string containing the usage report.
	 */
	private String generateUsageReport(String modelName, String finishReason, JsonNode usageNode) {
		StringBuilder responseStatistics = new StringBuilder();
		if (!verifyAllRequiredFieldsExist(usageNode)) {
			return responseStatistics.toString();
		}

		// Extract the actual model name from the full string
		String actualModelName = modelName.startsWith("Model: ") ?
				modelName.substring("Model: ".length()) : modelName;

		long promptTokens = usageNode.get("prompt_tokens").asLong();
		long completionTokens = usageNode.get("completion_tokens").asLong();
		long totalTokens = usageNode.get("total_tokens").asLong();

		// Retrieve model-specific data from ApiModelData
		String provider = ""; // Empty provider will match any provider in ApiModelData
		int maxInputTokens = ApiMetadata.getMaxInputTokens(actualModelName, provider);
		int maxOutputTokens = ApiMetadata.getMaxOutputTokens(actualModelName, provider);
		double inputCost = ApiMetadata.getInputCostPerToken(actualModelName, provider);
		double outputCost = ApiMetadata.getOutputCostPerToken(actualModelName, provider);

		responseStatistics.append(modelName).append("\n")
		.append(finishReason).append("\n");

		// Include percentages and costs if model data is available
		if (maxInputTokens > 0 || maxOutputTokens > 0) {
			appendTokenStats(responseStatistics, "Prompt", promptTokens, maxInputTokens, promptTokens * inputCost);
			appendTokenStats(responseStatistics, "Response", completionTokens, maxOutputTokens,
					completionTokens * outputCost);
			appendTokenStats(responseStatistics, "Total", totalTokens, maxInputTokens,
					promptTokens * inputCost + completionTokens * outputCost);
		} else {
			// Fallback to basic format if no model data found
			responseStatistics.append("Prompt: ").append(promptTokens).append(" tokens\n")
			.append("Response: ").append(completionTokens).append(" tokens\n")
			.append("Total: ").append(totalTokens).append(" tokens\n");
		}

		return responseStatistics.toString();
	}

	/**
	 * Appends token statistics to the given StringBuilder.
	 *
	 * @param sb The StringBuilder to append to.
	 * @param label The label for the statistic (e.g., "Prompt", "Response").
	 * @param tokens The number of tokens used.
	 * @param maxTokens The maximum number of tokens allowed (if available).
	 * @param cost The cost associated with the tokens (if available).
	 */
	private void appendTokenStats(StringBuilder sb, String label, long tokens, int maxTokens, double cost) {
		sb.append(label).append(": ").append(tokens).append(" tokens");

		if (maxTokens > 0 || cost > 0) {
			sb.append(" [");
			if (cost > 0) {
				int decimalPlaces = Math.max(2, -1 * (int)Math.floor(Math.log10(cost)) + 1);
				sb.append(String.format("$%." + decimalPlaces + "f", cost));
			}
			if (maxTokens > 0) {
				if (cost > 0) {
					sb.append(", ");
				}
				sb.append(String.format(" %.1f%%", (tokens * 100.0) / maxTokens));
			}
			sb.append("]");
		}
		sb.append("\n");
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

	/**
	 * These are used to determine if the given model is an OpenAI "o" (or "gpt-5") model with limited capabilities.
	 */
	private static boolean isOpenAiReasoningModel(String modelName) {
		return modelName.matches("^(openai/)?(o\\d|gpt-5)(-.*)?");
	}
	private static boolean isLegacyOpenAiReasoningModel(String modelName) {
		return modelName.matches("^(openai/)?o1-preview.*") || modelName.matches("^(openai/)?o1-mini-2024-09-12");
	}

}
