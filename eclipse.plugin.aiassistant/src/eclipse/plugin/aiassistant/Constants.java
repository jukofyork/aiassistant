package eclipse.plugin.aiassistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eclipse.plugin.aiassistant.preferences.BookmarkedApiSettings;

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

	// API base URL and endpoints.
	public static final String DEFAULT_MODEL_NAME = "gpt-4-turbo";
	public static final String DEFAULT_API_URL = "https://api.openai.com/v1";
	public static final String DEFAULT_API_KEY = "<YOUR KEY HERE>";
	public static final String MODEL_LIST_API_URL = "/models";
	public static final String CHAT_COMPLETION_API_URL = "/chat/completions";
	
	// Default bookmarked settings, showing 5 of the most common OpenAI compatible end-points.
	public static final List<BookmarkedApiSettings> DEFAULT_BOOKMARKED_API_SETTINGS = new ArrayList<>(Arrays.asList(
		new BookmarkedApiSettings("gpt-4-turbo", "https://api.openai.com/v1", "<YOUR API KEY>", 0.0),
		new BookmarkedApiSettings("anthropic/claude-3.5-sonnet", "https://openrouter.ai/api/v1","<YOUR API KEY>", 0.0),
		new BookmarkedApiSettings("<LLAMA.CPP MODEL NAME>", "http://localhost:8080/v1", "none", 0.0),
		new BookmarkedApiSettings("<OLLAMA MODEL NAME>", "http://localhost:11434/v1", "none", 0.0),
		new BookmarkedApiSettings("<TABBYAPI MODEL NAME>", "http://localhost:5000/v1", "none", 0.0)
	));

	// Widget dimensions and spacing for the main view.
	public static final int DEFAULT_EXTERNAL_MARGINS = 0;
	public static final int DEFAULT_INTERNAL_SPACING = 2;

	// =============================================================================
	
	// Connection timeout.
	// NOTE: A short connection timeout stops the preference page from stalling.
	public static final int MIN_CONNECTION_TIMEOUT = 1;
	public static final int MAX_CONNECTION_TIMEOUT = 10;
	public static final int DEFAULT_CONNECTION_TIMEOUT = 1;

	// Response timeout.
	// NOTE: The new `o1` models can take ages to reply... 5 minutes should hopefully be enough.
	public static final int MIN_REQUEST_TIMEOUT = 5;
	public static final int MAX_REQUEST_TIMEOUT = Integer.MAX_VALUE;
	public static final int DEFAULT_REQUEST_TIMEOUT = 300;

	// Temperature value.
	// NOTE: Coding LLMs need a much lower (preferably zero) temperature vs chat LLMs.
	public static final double MIN_TEMPERATURE = 0.0;		// Zero temperature.
	public static final double MAX_TEMPERATURE = 2.0;		// Max sane value.
	public static final double DEFAULT_TEMPERATURE = 0.0;

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
	public static final boolean DEFAULT_DISABLE_TOOLTIPS = true;

}