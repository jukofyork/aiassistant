package eclipse.plugin.aiassistant.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

public class OpenAIChatCompletionClient {

	private final HttpClient httpClient;
	
	// Created on call to subscribe() and set null after closing.
	private SubmissionPublisher<String> streamingResponsePublisher = null;

	private Supplier<Boolean> isCancelled = () -> false;

	public OpenAIChatCompletionClient() {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Preferences.getConnectionTimeout()))
				.build();
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
	 * Creates and returns a Runnable that will execute the HTTP request to OpenAI
	 * API with the given conversation prompt and process the responses.
	 * 
	 * @param prompt The conversation to be sent to the OpenAI API.
	 * @return A Runnable that performs the HTTP request and processes the
	 *         responses.
	 */
	public Runnable run(ChatConversation chatConversation) {
		return () -> {
			try {
				HttpResponse<InputStream> streamingResponse = sendChatCompletionRequest(chatConversation);
				processStreamingResponse(streamingResponse);
			} catch (Exception e) {
				if (streamingResponsePublisher != null)
					streamingResponsePublisher.closeExceptionally(e);
				streamingResponsePublisher = null;
				throw new RuntimeException(e);
			} finally {
				if (streamingResponsePublisher != null)
					streamingResponsePublisher.close();
				streamingResponsePublisher = null;
			}
		};
	}

	/**
	 * Sends a chat completion request to the OpenAI API with the given conversation
	 * prompt.
	 * 
	 * @param chatConversation The conversation to be sent to the OpenAI API.
	 * @return The HTTP response from the OpenAI API.
	 * @throws Exception If an error occurs while sending the request.
	 */
	private HttpResponse<InputStream> sendChatCompletionRequest(ChatConversation chatConversation) {
		try {
			return sendRequest(Preferences.getChatCompletionApiEndpoint().toURI(),
					buildChatCompletionRequestBody(chatConversation));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds the request body for the chat completion request to the OpenAI API.
	 * 
	 * @param chatConversation The conversation to be sent to the OpenAI API.
	 * @return The request body as a string.
	 */
	private String buildChatCompletionRequestBody(ChatConversation chatConversation) {
		try {
			var objectMapper = new ObjectMapper();
			var requestBody = objectMapper.createObjectNode();
			var jsonMessages = objectMapper.createArrayNode();

			// Add the model name first.
			requestBody.put("model", Preferences.getApiModelName());

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

			// Add the temperature to the request.
			requestBody.put("temperature", Preferences.getTemperature());

			// Set the streaming flag.
			requestBody.put("stream", true);
			
			// Set the flag to get the usage statistics.
			var node = objectMapper.createObjectNode();
			node.put("include_usage", true);
			requestBody.putPOJO("stream_options", node);
			
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
			
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
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
			Logger.info("CANCELLED");
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
	 * Sends an HTTP request with a specified URI and body.
	 * 
	 * @param uri  The URI of the request.
	 * @param body The body of the request, or null if no body is required.
	 * @return The HttpResponse object containing the response data.
	 * @throws IOException If an error occurs while sending the request.
	 */
	private HttpResponse<InputStream> sendRequest(URI uri, String body) throws IOException {
		try {
			HttpRequest request = buildRequest(uri, body);
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() == 200) {
				return response;
			} else {
				Logger.warning("Request failed with status code " + response.statusCode());
			}
		} catch (Exception e) {
			Logger.error("Failed to send request", e);
		}
		throw new IOException("Failed to send request");
	}

	/**
	 * Builds an HttpRequest object with the specified URI and body.
	 * 
	 * @param uri  The URI of the request.
	 * @param body The body of the request, or null if no body is required.
	 * @return The built HttpRequest object.
	 * @throws IOException If an error occurs while building the request.
	 */
	private HttpRequest buildRequest(URI uri, String body) throws IOException {
		HttpRequest request;
		try {
			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(uri)// .timeout(REQUEST_TIMEOUT)
					.version(HttpClient.Version.HTTP_1_1)
					.header("Authorization", "Bearer " + Preferences.getApiKey())
					.header("Accept", "text/event-stream")
					.header("Content-Type", "application/json");
			if (body == null || body.isEmpty()) {
				requestBuilder.GET();
			} else {
				requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
			}
			request = requestBuilder.build();
		} catch (Exception e) {
			Logger.warning("Could not build the request", e);
			throw new IOException("Could not build the request");
		}
		return request;
	}
}
