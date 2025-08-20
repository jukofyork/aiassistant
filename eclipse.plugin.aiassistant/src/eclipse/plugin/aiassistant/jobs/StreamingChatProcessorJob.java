package eclipse.plugin.aiassistant.jobs;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eclipse.plugin.aiassistant.Activator;
import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.chat.ChatConversation;
import eclipse.plugin.aiassistant.chat.ChatMessage;
import eclipse.plugin.aiassistant.network.OpenAiApiClient;
import eclipse.plugin.aiassistant.view.MainPresenter;

/**
 * Job responsible for processing streaming chat responses from the OpenAI API.
 * It subscribes to responses and updates the UI accordingly through the MainPresenter.
 * Implements the Subscriber interface to handle streamed data.
 */
public class StreamingChatProcessorJob extends Job implements Subscriber<String> {

	private final MainPresenter mainPresenter;
	private final OpenAiApiClient openAiApiClient;

	private ChatConversation currentChatConversation = null;

	private ChatMessage message;

	private Subscription subscription;

	/**
	 * Initializes a new job to process chat conversations using OpenAI's API.
	 *
	 * @param mainPresenter           Controller for updating the chat UI.
	 * @param openAiApiClient Client for OpenAI chat API.
	 * @param chatConversation        The conversation context.
	 */
	public StreamingChatProcessorJob(MainPresenter mainPresenter, OpenAiApiClient openAiApiClient) {
		super(Activator.getDefault().getBundle().getSymbolicName());
		this.mainPresenter = mainPresenter;
		this.openAiApiClient = openAiApiClient;
	}

	public synchronized void setConversation(ChatConversation chatConversation) {
		this.currentChatConversation = chatConversation;
	}

	/**
	 * Executes the job of subscribing to and processing chat data.
	 *
	 * Note: setConversation(ChatConversation) MUST be called before executing this job.
	 *
	 * @param progressMonitor Monitors the progress of the job.
	 * @return Status of the job execution.
	 */
	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {
		if (currentChatConversation == null) {
			return Status.error("No chat conversation set", null);
		}

		openAiApiClient.subscribe(this);
		openAiApiClient.setCancelProvider(() -> progressMonitor.isCanceled());
		try {
			var future = CompletableFuture.runAsync(openAiApiClient.run(currentChatConversation)).thenApply(v -> Status.OK_STATUS)
					.exceptionally(e -> Status.error("Unable to run the task: " + e.getMessage(), e));
			return future.get();
		} catch (Exception e) {
			return Status.error(e.getMessage(), e);
		}
	}

	/**
	 * Handles the subscription setup with the publisher.
	 *
	 * @param subscription The subscription to manage data flow.
	 */
	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		message = mainPresenter.beginMessageFromAssistant();
		subscription.request(1);
	}

	/**
	 * Processes each new message received from the OpenAI API.
	 *
	 * @param item The message part received.
	 */
	@Override
	public void onNext(String item) {
		Objects.requireNonNull(message);
		Objects.requireNonNull(subscription);
		message.appendContent(item);
		mainPresenter.updateMessageFromAssistant(message);
		subscription.request(1);
	}

	/**
	 * Handles errors during the subscription.
	 *
	 * @param throwable The exception thrown during streaming.
	 */
	@Override
	public void onError(Throwable throwable) {
		mainPresenter.endMessageFromAssistant();
		subscription.cancel();
		if (throwable instanceof CancellationException) {
			Logger.info("CANCELLED");
		}
		else {
			Logger.error(throwable.getMessage());
		}
	}

	/**
	 * Completes the message processing and updates the UI.
	 */
	@Override
	public void onComplete() {
		mainPresenter.endMessageFromAssistant();
		subscription.cancel();
	}

}