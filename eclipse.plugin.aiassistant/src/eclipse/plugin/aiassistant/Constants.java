package eclipse.plugin.aiassistant;

import java.time.Duration;

public final class Constants {

	/**
	 * Private constructor to prevent instantiation of the class.
	 */
	private Constants() {
	}

	// Plugin paths.
	public static final String PLUGIN_BASE_PATH = "platform:/plugin/eclipse.plugin.aiassistant";
	public static final String PROMPTS_PATH = PLUGIN_BASE_PATH + "/prompts/";
	public static final String ICONS_PATH = PLUGIN_BASE_PATH + "/icons/";
	public static final String JS_PATH = PLUGIN_BASE_PATH + "/js/";
	public static final String CSS_PATH = PLUGIN_BASE_PATH + "/css/";
	public static final String JSON_PATH = PLUGIN_BASE_PATH + "/json/";

	// The CSS and JS files we will load to initialise the browser.
	public static final String[] CSS_FILENAMES = { "main-style.css", "code-block-header.css", "code-block-style.css" };
	public static final String[] JS_FILENAMES = { "highlight.min.js" };
	
	// Contains all highlight.js's supported languages and file extensions.
	// See: https://github.com/highlightjs/highlight.js/blob/main/SUPPORTED_LANGUAGES.md
	public static final String HIGHLIGHT_JS_LANGUAGES_FILENAMES = "language-extensions.json";

	// Ollama server keep-alive interval.
	// See: https://github.com/ollama/ollama/blob/main/server/routes.go
	public static final Duration OLLAMA_KEEP_ALIVE_INTERVAL = Duration.ofSeconds((long) (4.5 * 60));

	// API base URL and endpoints.
	// NOTE: Also check 'OLLAMA_HOST' and 'OLLAMA_ORIGINS' environment variables
	public static final String DEFAULT_API_BASE_URL = "http://127.0.0.1:11434";
	public static final String COMPLETION_API_URL = "/api/generate";
	public static final String CHAT_COMPLETION_API_URL = "/api/chat";
	public static final String MODEL_LIST_API_URL = "/api/tags";

	// Widget dimensions and spacing for the main view.
	public static final int DEFAULT_EXTERNAL_MARGINS = 0;
	public static final int DEFAULT_INTERNAL_SPACING = 2;

	// =============================================================================

	// Connection timeout.
	// NOTE: A short connection timeout stops the preference page from stalling.
	// NOTE: A general timeout not used as slow models can take ages to reply.
	public static final int MIN_CONNECTION_TIMEOUT = 500;
	public static final int MAX_CONNECTION_TIMEOUT = 10000;
	public static final int DEFAULT_CONNECTION_TIMEOUT = 1000;

	// Temperature value.
	// NOTE: Coding LLMs need a much lower (preferably zero) temperature vs chat LLMs.
	public static final double MIN_TEMPERATURE = 0.0;		// Zero temperature.
	public static final double MAX_TEMPERATURE = 2.0;		// Max sane value.
	public static final double DEFAULT_TEMPERATURE = 0.0;

	// Repeat penalty value.
	// NOTE: Coding LLMs need a much lower (preferably no) repetition penalty scaler.
	// NOTE: Increase very slowly from 1 and only if repetition problems occur.
	public static final double MIN_REPEAT_PENALTY_VALUE = 1.0;		// No penalty.
	public static final double MAX_REPEAT_PENALTY_VALUE = 1.5;		// Max sane value.
	public static final double DEFAULT_REPEAT_PENALTY_VALUE = 1.0;
	
	// Repeat penalty window size.
	// NOTE: Coding LLMs may sometimes need a larger repetition penalty window.
	// NOTE: Double each time they start looping when writing out lists, etc.
	public static final int MIN_REPEAT_PENALTY_WINDOW = -1;		// Full context.
	public static final int MAX_REPEAT_PENALTY_WINDOW = 65536;	// Max sane value.
	public static final int DEFAULT_REPEAT_PENALTY_WINDOW = 64;

	// Font sizes.
	// NOTE: These are just substituted in "main-style.css" using a regex currently.
	public static final int MIN_CHAT_FONT_SIZE = 8;
	public static final int MAX_CHAT_FONT_SIZE = 16;
	public static final int DEFAULT_CHAT_FONT_SIZE = 14;
	public static final int MIN_NOTIFICATION_FONT_SIZE = 8;
	public static final int MAX_NOTIFICATION_FONT_SIZE = 16;
	public static final int DEFAULT_NOTIFICATION_FONT_SIZE = 10;

	// Miscellaneous checkbox settings.
	public static final boolean DEFAULT_USE_STREAMING = true;
	public static final boolean DEFAULT_USE_KEEPALIVE_SERVICE = true;
	public static final boolean DEFAULT_DISABLE_TOOLTIPS = false;

}