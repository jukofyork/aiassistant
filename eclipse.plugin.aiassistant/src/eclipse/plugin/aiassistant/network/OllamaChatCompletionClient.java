package eclipse.plugin.aiassistant.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.chat.ChatRole;
import eclipse.plugin.aiassistant.preferences.Preferences;
import eclipse.plugin.aiassistant.prompt.PromptLoader;

/**
 * This class is responsible for managing interactions with the Ollama API
 * related to chat-completion. It uses HTTP client helper to send requests and
 * processes responses from the Ollama API.
 */
public class OllamaChatCompletionClient {

	private final HttpClientWrapper httpClientHelper;
	
	// Created on call to subscribe() and set null after closing.
	private SubmissionPublisher<String> streamingResponsePublisher = null;

	private Supplier<Boolean> isCancelled = () -> false;

	public OllamaChatCompletionClient() {
		httpClientHelper = new HttpClientWrapper();
	}

	/**
	 * Subscribes a subscriber to receive streaming String data the Ollama API.
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
	 * Creates and returns a Runnable that will execute the HTTP request to Ollama
	 * API with the given conversation prompt and process the responses.
	 * 
	 * @param prompt The conversation to be sent to the Ollama API.
	 * @return A Runnable that performs the HTTP request and processes the
	 *         responses.
	 */
	public Runnable run(ChatConversation chatConversation) {
		return () -> {
			try {
				HttpResponse<InputStream> streamingResponse = sendChatCompletionRequest(chatConversation);
				processStreamingResponse(streamingResponse);
			} catch (Exception e) {
				streamingResponsePublisher.closeExceptionally(e);
				streamingResponsePublisher = null;
				throw new RuntimeException(e);
			} finally {
				streamingResponsePublisher.close();
				streamingResponsePublisher = null;
			}
		};
	}

	/**
	 * Sends a chat completion request to the Ollama API with the given conversation
	 * prompt.
	 * 
	 * @param chatConversation The conversation to be sent to the Ollama API.
	 * @return The HTTP response from the Ollama API.
	 * @throws Exception If an error occurs while sending the request.
	 */
	private HttpResponse<InputStream> sendChatCompletionRequest(ChatConversation chatConversation) {
		try {
			return httpClientHelper.sendRequest(Preferences.getChatCompletionApiEndpoint().toURI(),
					buildChatCompletionRequestBody(chatConversation));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds the request body for the chat completion request to the Ollama API.
	 * 
	 * @param chatConversation The conversation to be sent to the Ollama API.
	 * @return The request body as a string.
	 */
	private String buildChatCompletionRequestBody(ChatConversation chatConversation) {
		try {
			var objectMapper = new ObjectMapper();
			var requestBody = objectMapper.createObjectNode();
			var jsonMessages = objectMapper.createArrayNode();

			// Add the model name first.
			requestBody.put("model", Preferences.getLastSelectedModelName());

			// Add the message history so far.
			var systemMessage = objectMapper.createObjectNode();
			systemMessage.put("role", "system");
			systemMessage.put("content", PromptLoader.getSystemPromptText());
			jsonMessages.add(systemMessage);
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

			// Add the options to the request.
			var options = objectMapper.createObjectNode();
			options.put("temperature", Preferences.getTemperature());
			options.put("repeat_penalty", Preferences.getRepeatPenaltyValue());
			options.put("repeat_last_n", Preferences.getRepeatPenaltyWindow());
			requestBody.set("options", options);

			// Set the streaming flag.
			if (Preferences.useStreaming()) {
				requestBody.put("stream", true);
			} else {
				requestBody.put("stream", false);
			}
			
			//Logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Processes the streaming response from the Ollama API.
	 * 
	 * @param response The HTTP response from the Ollama API.
	 * @throws IOException If an error occurs while processing the response.
	 */
	private void processStreamingResponse(HttpResponse<InputStream> response) throws IOException {

		try (var inputStream = response.body();
				var streamingInputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				var streamingResponseReader = new BufferedReader(streamingInputStreamReader)) {

			// These are used so we don't overload the publisher with too many calls.
			StringBuilder responseContentBuffer = new StringBuilder();

			// Read each streamed packet as we get them from Ollama.
			String line;
			while ((line = streamingResponseReader.readLine()) != null && !isCancelled.get()) {

				// Get the streamed content and append to the buffer.
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonTree = mapper.readTree(line);
				String messageContent = extractMessageContent(jsonTree);
				if (messageContent != null) {
					responseContentBuffer.append(messageContent);
				}

				// If the queue is empty, then let it deal with the buffered string we have
				// created.
				if (streamingResponsePublisher.estimateMaximumLag() == 0) {
					streamingResponsePublisher.submit(responseContentBuffer.toString());
					responseContentBuffer.setLength(0);
					// lastResponseSubmissionTime = currentSystemTime;
				}

				// If we are done then submit the buffer and print the stats.
				if (isStreamingFinished(jsonTree)) {
					streamingResponsePublisher.submit(responseContentBuffer.toString());
					Logger.info(generateResponseStatistics(jsonTree));
				}

			}

		}
		if (isCancelled.get()) {
			Logger.info("CANCELLED");
			streamingResponsePublisher.closeExceptionally(new CancellationException());
		}
	}

	/**
	 * Extracts the message content from a JSON tree.
	 * 
	 * @param jsonTree The JSON tree to extract the message content from.
	 * @return The message content, or null if it does not exist.
	 */
	private String extractMessageContent(JsonNode jsonTree) {
		if (jsonTree.has("message") && jsonTree.get("message").has("content")) {
			var content = jsonTree.get("message").get("content").asText();
			return content;
		}
		return null;
	}

	/**
	 * Checks if the streaming is finished.
	 * 
	 * @param jsonTree The JSON tree to check.
	 * @return True if the streaming is finished, false otherwise.
	 */
	private boolean isStreamingFinished(JsonNode jsonTree) {
		return jsonTree.has("done") && jsonTree.get("done").asBoolean() == true;
	}

	/**
	 * Generates response statistics from a JSON tree.
	 * 
	 * @param jsonTree The JSON tree to generate the statistics from.
	 * @return The response statistics as a string.
	 */
	private String generateResponseStatistics(JsonNode jsonTree) {
		StringBuilder responseStatistics = new StringBuilder();
		if (verifyAllRequiredFieldsExist(jsonTree)) {
			long totalDuration = jsonTree.get("total_duration").asLong();
			long loadDuration = jsonTree.get("load_duration").asLong();
			long promptEvalCount = jsonTree.get("prompt_eval_count").asLong();
			long promptEvalDuration = jsonTree.get("prompt_eval_duration").asLong();
			long evalCount = jsonTree.get("eval_count").asLong();
			long evalDuration = jsonTree.get("eval_duration").asLong();
			responseStatistics
					.append("Loading: ")
					.append(formatDuration(loadDuration))
					.append("\n")
					.append("Input: ")
					.append(promptEvalCount)
					.append(" tokens (")
					.append(formatDuration(promptEvalDuration))
					.append(" @ ")
					.append(formatFrequency(promptEvalCount, promptEvalDuration))
					.append(")\n")					
					.append("Output: ")
					.append(evalCount)
					.append(" tokens (")
					.append(formatDuration(evalDuration))
					.append(" @ ")
					.append(formatFrequency(evalCount, evalDuration))
					.append(")\n")					
					.append("Total: ")
					.append(promptEvalCount + evalCount)
					.append(" tokens (")
					.append(formatDuration(totalDuration))
					.append(" @ ")
					.append(formatFrequency(promptEvalCount + evalCount, promptEvalDuration + evalDuration))
					.append(")\n");
		}
		return responseStatistics.toString();
	}

	/**
	 * Verifies that all required fields exist in a JSON tree.
	 * 
	 * @param jsonTree The JSON tree to verify.
	 * @return True if all required fields exist, false otherwise.
	 */
	private boolean verifyAllRequiredFieldsExist(JsonNode jsonTree) {
		return jsonTree.has("total_duration") && jsonTree.has("load_duration") && jsonTree.has("prompt_eval_count")
				&& jsonTree.has("prompt_eval_duration") && jsonTree.has("eval_count") && jsonTree.has("eval_duration");
	}

	/**
	 * Formats a frequency from a count and duration.
	 * 
	 * @param count    The count to format.
	 * @param duration The duration to format.
	 * @return The formatted frequency as a string.
	 */
	private String formatFrequency(long count, long duration) {
		return String.format("%.1f tokens/s", (double) count / duration * 1e9);
	}

	/**
	 * Formats a duration.
	 * 
	 * @param duration The duration to format.
	 * @return The formatted duration as a string.
	 */
	private String formatDuration(long duration) {
		if (duration >= 1e9) {
			return String.format("%.1f s", (double) duration / 1e9);
		} else if (duration >= 1e6) {
			return String.format("%.1f ms", (double) duration / 1e6);
		} else if (duration >= 1e3) {
			return String.format("%.1f μs", (double) duration / 1e3);
		} else {
			return String.format("%d ns", duration);
		}
	}

}
