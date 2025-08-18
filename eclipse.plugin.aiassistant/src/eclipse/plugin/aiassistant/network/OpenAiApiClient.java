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
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

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
	 * Handles special cases for different model types, includes
	 * appropriate configuration based on model capabilities, and
	 * applies any JSON overrides specified in preferences.
	 *
	 * @param modelName The name of the model to use
	 * @param chatConversation The conversation to process
	 * @return JSON string containing the request body
	 * @throws Exception if request body creation fails or JSON overrides are invalid
	 */
	private String buildChatCompletionRequestBody(String modelName, ChatConversation chatConversation) throws Exception {
		try {
			var objectMapper = new ObjectMapper();
			var requestBody = objectMapper.createObjectNode();
			var jsonMessages = objectMapper.createArrayNode();

			// Add the model ID first.
			requestBody.put("model", modelName);

			// Add the system (or developer message for OpenAI reasoning models).
			if (Preferences.getCurrentUseSystemMessage()) {
				var systemMessage = objectMapper.createObjectNode();
				systemMessage.put("role", "system");
				systemMessage.put("content", PromptLoader.getSystemPromptText());
				jsonMessages.add(systemMessage);
			} else if (Preferences.getCurrentUseDeveloperMessage()) {
				var developerMessage = objectMapper.createObjectNode();
				developerMessage.put("role", "developer");
				developerMessage.put("content", PromptLoader.getDeveloperPromptText());
				jsonMessages.add(developerMessage);
			}

			// Add the message history so far.
			for (ChatMessage message : chatConversation.messagesExcludingLastIfEmpty()) {
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

			// Set the streaming flag.
			if (Preferences.getCurrentUseStreaming()) {
				requestBody.put("stream", true);
				var node = objectMapper.createObjectNode();
				node.put("include_usage", true);
				requestBody.putPOJO("stream_options", node);
			}

			// Apply JSON overrides if specified
			String jsonOverrides = Preferences.getCurrentJsonOverrides();
			if (jsonOverrides != null && !jsonOverrides.trim().isEmpty()) {
				try {
					// Wrap with braces since JsonFieldEditor validates without outer braces
					String wrappedOverrides = "{ " + jsonOverrides.trim() + " }";
					JsonNode overridesNode = null;

					// Try JSON first
					try {
						overridesNode = objectMapper.readTree(wrappedOverrides);
					} catch (JsonProcessingException e) {
						// JSON parsing failed, try TOML
						try {
							ObjectMapper tomlMapper = new ObjectMapper(new TomlFactory());
							String wrappedToml = "temp = " + wrappedOverrides;
							JsonNode tomlTree = tomlMapper.readTree(wrappedToml);
							overridesNode = tomlTree.get("temp");
						} catch (JsonProcessingException tomlException) {
							throw new Exception("Failed to parse JSON overrides as either JSON or TOML: " + e.getMessage(), e);
						}
					}

					// Apply each override field to the request body
					if (overridesNode != null) {
						overridesNode.fields().forEachRemaining(entry -> {
							requestBody.set(entry.getKey(), entry.getValue());
						});
					}
				} catch (JsonProcessingException e) {
					throw new Exception("Failed to parse JSON overrides: " + e.getMessage(), e);
				}
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
				modelName = jsonTree.get("model").asText();
			}

			if (jsonTree.has("choices") && jsonTree.get("choices").has(0)) {
				JsonNode choiceNode = jsonTree.get("choices").get(0);
				if (choiceNode.has("finish_reason")) {
					finishReason = choiceNode.get("finish_reason").asText();
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
							modelName = jsonTree.get("model").asText();
						}

						// Get the finish reason and/or content from the "choices" sub-tree.
						if (jsonTree.has("choices") && jsonTree.get("choices").has(0)) {
							var choiceNode = jsonTree.get("choices").get(0);
							if (choiceNode.has("finish_reason") && !choiceNode.get("finish_reason").asText().equals("null")) {
								finishReason = choiceNode.get("finish_reason").asText();
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
	 * @return The text content, or null if: the field doesn't exist, is null or is empty
	 */
	private String extractFieldText(JsonNode node, String fieldName) {
		if (node.has(fieldName)) {
			JsonNode fieldNode = node.get(fieldName);
			// NOTE: Check for JSON null so string "null" will be preserved!
			if (!fieldNode.isNull()) {
				String fieldContent = fieldNode.asText();
				// NOTE: Check for only empty strings (ie: isBlank() would also exclude whitespace!).
				if (!fieldContent.isEmpty()) {
					return fieldContent;
				}
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

		long promptTokens = usageNode.get("prompt_tokens").asLong();
		long completionTokens = usageNode.get("completion_tokens").asLong();

		// Extract cached tokens if available
		long cachedTokens = 0;
		if (usageNode.has("prompt_tokens_details")) {
			JsonNode promptDetails = usageNode.get("prompt_tokens_details");
			if (promptDetails.has("cached_tokens")) {
				cachedTokens = promptDetails.get("cached_tokens").asLong();
			}
		}

		// Extract reasoning tokens if available
		long reasoningTokens = 0;
		if (usageNode.has("completion_tokens_details")) {
			JsonNode completionDetails = usageNode.get("completion_tokens_details");
			if (completionDetails.has("reasoning_tokens")) {
				reasoningTokens = completionDetails.get("reasoning_tokens").asLong();
			}
		}

		// Retrieve model-specific data from ApiModelData
		String provider = ""; // Empty provider will match any provider in ApiModelData
		int maxInputTokens = ApiMetadata.getMaxInputTokens(modelName, provider);
		int maxOutputTokens = ApiMetadata.getMaxOutputTokens(modelName, provider);
		double inputCost = ApiMetadata.getInputCostPerToken(modelName, provider);
		double outputCost = ApiMetadata.getOutputCostPerToken(modelName, provider);

		responseStatistics.append("Model: \"").append(modelName).append("\"\n");
		responseStatistics.append("Finish: \"").append(finishReason).append("\"\n");

		// Prompt line
		appendTokenLine(responseStatistics, "Prompt", promptTokens, maxInputTokens,
				cachedTokens > 0 ? "Cached: " + cachedTokens + " tokens" : null);

		// Response line - subtract reasoning tokens since they don't count towards output limit
		long actualResponseTokens = completionTokens - reasoningTokens;
		appendTokenLine(responseStatistics, "Response", actualResponseTokens, maxOutputTokens,
				reasoningTokens > 0 ? "Reasoning: " + reasoningTokens + " tokens" : null);

		// Cost line (only if cost data available)
		double promptCost = promptTokens * inputCost;
		double responseCost = (completionTokens + reasoningTokens) * outputCost;
		double totalCost = promptCost + responseCost;

		if (totalCost > 0) {
			appendChargeLine(responseStatistics, promptCost, responseCost, totalCost);
		}

		return responseStatistics.toString();
	}

	/**
	 * Appends a formatted token usage line to the provided StringBuilder.
	 * The line includes the token count and optionally shows the percentage
	 * of context used and additional information in brackets.
	 *
	 * @param sb the StringBuilder to append the formatted line to
	 * @param label the descriptive label for this token type (e.g., "Prompt", "Response")
	 * @param tokens the number of tokens used
	 * @param maxTokens the maximum token limit for percentage calculation;
	 *                  if 0 or negative, no percentage is shown
	 * @param additionalInfo optional extra information to display in brackets;
	 *                       if null, no brackets are added
	 */
	private void appendTokenLine(StringBuilder sb, String label, long tokens, int maxTokens, String additionalInfo) {
		sb.append(label).append(": ").append(tokens).append(" tokens");

		if (maxTokens > 0) {
			double percent = (tokens * 100.0) / maxTokens;
			long roundedPercent = Math.round(percent);

			if (roundedPercent == 0 && percent > 0) {
				// Use 1 significant figure to avoid showing "0%" for tiny non-zero values
				String formatted = String.format("%.1g", percent);
				sb.append(" (").append(formatted).append("%)");
			} else {
				sb.append(" (").append(roundedPercent).append("%)");
			}
		}

		if (additionalInfo != null) {
			sb.append(" (").append(additionalInfo).append(")");
		}

		sb.append("\n");
	}

	/**
	 * Appends a formatted charge line to the provided StringBuilder.
	 * Shows the total charge and optionally includes a breakdown of prompt
	 * and response charges when both are positive values.
	 *
	 * @param sb the StringBuilder to append the formatted charge line to
	 * @param promptCost the charge associated with prompt tokens
	 * @param responseCost the charge associated with response tokens
	 * @param totalCost the total charge (should equal promptCost + responseCost)
	 */
	private void appendChargeLine(StringBuilder sb, double promptCost, double responseCost, double totalCost) {
		sb.append("Charge: ");

		if (promptCost > 0 && responseCost > 0) {
			sb.append(formatCurrency(totalCost))
			.append(" (")
			.append(formatCurrency(promptCost))
			.append(" + ")
			.append(formatCurrency(responseCost))
			.append(")");
		} else {
			sb.append(formatCurrency(totalCost));
		}

		sb.append("\n");
	}

	/**
	 * Formats a monetary amount using an appropriate currency symbol and precision.
	 * Uses cents (￠) for amounts less than $1.00 and a fullwidth dollar sign (＄)
	 * for larger amounts. The fullwidth dollar sign prevents conflicts with LaTeX
	 * math mode delimiters when this output is displayed in contexts that process LaTeX.
	 *
	 * @param amount the monetary amount to format
	 * @return a formatted currency string (e.g., "25￠", "＄1.50")
	 */
	private String formatCurrency(double amount) {
		if (amount == 0) {
			return "0￠";
		}

		if (amount < 1.0) {
			// Convert to cents
			double cents = amount * 100;

			// Check if rounding to nearest whole cent would give us 0
			if (Math.round(cents) == 0) {
				// Format with 1 significant figure to avoid showing "0￠" for tiny amounts
				String formatted = String.format("%.1g", cents);
				return formatted + "￠";
			} else {
				// Round to nearest whole cent
				return String.format("%.0f￠", cents);
			}
		} else {
			// Use fullwidth dollar sign with 2 decimal places fixed
			return String.format("＄%.2f", amount);
		}
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
