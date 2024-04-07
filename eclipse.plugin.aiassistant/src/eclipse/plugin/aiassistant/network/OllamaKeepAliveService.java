package eclipse.plugin.aiassistant.network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import eclipse.plugin.aiassistant.Constants;
import eclipse.plugin.aiassistant.preferences.Preferences;

/**
 * The OllamaKeepAliveService class is responsible for keeping the connection to
 * the Ollama server alive. It does this by periodically attempting to load the
 * last selected model into memory.
 */
public class OllamaKeepAliveService implements Runnable {

	/**
	 * The executor service that schedules the keep-alive task.
	 */
	private final ScheduledExecutorService executorService;

	/**
	 * The OllamaModelManager instance responsible for managing the models.
	 */
	private final OllamaModelManager ollamaModelManager;

	/**
	 * Constructs a new OllamaKeepAliveService.
	 * 
	 * This constructor initializes the OllamaModelManager and
	 * ScheduledExecutorService, and schedules the keep-alive task to run
	 * periodically at a fixed rate.
	 */
	public OllamaKeepAliveService() {
		this.ollamaModelManager = new OllamaModelManager();
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		this.executorService.scheduleAtFixedRate(this, 0, Constants.OLLAMA_KEEP_ALIVE_INTERVAL.getSeconds(),
				TimeUnit.SECONDS);
	}

	/**
	 * If the preference 'useSendKeepaliveService' is set to true, then attempts to
	 * load the last selected model into memory. Otherwise, it does nothing.
	 */
	@Override
	public void run() {
		if (Preferences.useKeepaliveService()) {
			ollamaModelManager.attemptLoadLastSelectedModelIntoMemory();
		}
	}

	/**
	 * Shuts down the executor service, waiting up to 10 seconds for tasks to
	 * complete before forcefully terminating them.
	 */
	public void shutdown() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}
}