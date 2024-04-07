package eclipse.plugin.aiassistant.jobs;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eclipse.plugin.aiassistant.Activator;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.network.OllamaChatCompletionClient;
import eclipse.plugin.aiassistant.view.MainPresenter;

/**
 * This class represents a job that processes streaming chat completion using
 * the Ollama API. It extends the Job class and implements the Flow.Subscriber
 * interface to handle the streamed responses from the API.
 */
public class StreamingChatProcessorJob extends Job implements Subscriber<String> {

	private final MainPresenter mainPresenter;
	private final OllamaChatCompletionClient ollamaChatCompletionClient;
	private final ChatConversation chatConversation;
	
	private ChatMessage message;

	private Subscription subscription;

	/**
	 * Constructs a new StreamingChatProcessorJob with the given client provider,
	 * chat conversation manager, and chat conversation.
	 * 
	 * @param clientProvider          The provider of the
	 *                                OllamaStreamingChatCompletionClient.
	 * @param mainPresenter           The manager for the chat conversation.
	 * @param chatConversation        The chat conversation to be processed.
	 */
	public StreamingChatProcessorJob(MainPresenter mainPresenter, OllamaChatCompletionClient ollamaChatCompletionClient,
			ChatConversation chatConversation) {
		super(Activator.getDefault().getBundle().getSymbolicName());
		this.mainPresenter = mainPresenter;
		this.ollamaChatCompletionClient = ollamaChatCompletionClient;
		this.chatConversation = chatConversation;
	}

	/**
	 * Runs the job and processes the streaming chat completion using the Ollama
	 * API.
	 * 
	 * @param progressMonitor The progress monitor for the job.
	 * @return The status of the job after it has finished running.
	 */
	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {
		ollamaChatCompletionClient.subscribe(this);
		ollamaChatCompletionClient.setCancelProvider(() -> progressMonitor.isCanceled());
		try {
			var future = CompletableFuture.runAsync(ollamaChatCompletionClient.run(chatConversation)).thenApply(v -> Status.OK_STATUS)
					.exceptionally(e -> Status.error("Unable to run the task: " + e.getMessage(), e));
			return future.get();
		} catch (Exception e) {
			return Status.error(e.getMessage(), e);
		}
	}

	/**
	 * Called when a subscription is established with the publisher.
	 * 
	 * @param subscription The subscription to the publisher.
	 */
	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		message = mainPresenter.beginMessageFromAssistant();
		subscription.request(1);
	}

	/**
	 * Called when a new item is received from the publisher.
	 * 
	 * @param item The new item received from the publisher.
	 */
	@Override
	public void onNext(String item) {
		Objects.requireNonNull(message);
		Objects.requireNonNull(subscription);
		message.appendMessage(item);
		mainPresenter.updateMessageFromAssistant(message);
		subscription.request(1);
	}

	/**
	 * Called when an error occurs in the publisher.
	 * 
	 * @param throwable The error that occurred.
	 */
	@Override
	public void onError(Throwable throwable) {
		// handle error
	}

	/**
	 * Called when the publisher has completed sending items.
	 */
	@Override
	public void onComplete() {
		mainPresenter.endMessageFromAssistant();
		subscription.request(1);
	}

}