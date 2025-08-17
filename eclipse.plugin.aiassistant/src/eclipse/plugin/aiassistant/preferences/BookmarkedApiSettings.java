package eclipse.plugin.aiassistant.preferences;

import java.io.Serializable;
import java.util.Objects;

public class BookmarkedApiSettings implements Serializable, Comparable<BookmarkedApiSettings> {
	private static final long serialVersionUID = 1L;

	private String modelName;
	private String apiUrl;
	private String apiKey;
	private String jsonOverrides;
	private boolean useStreaming;
	private boolean useSystemMessage;
	private boolean useDeveloperMessage;

	public BookmarkedApiSettings(String modelName, String apiUrl, String apiKey, String jsonOverrides,
			boolean useStreaming, boolean useSystemMessage, boolean useDeveloperMessage) {
		this.modelName = modelName;
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.jsonOverrides = jsonOverrides;
		this.useStreaming = useStreaming;
		this.useSystemMessage = useSystemMessage;
		this.useDeveloperMessage = useDeveloperMessage;
	}

	public String getModelName() { return modelName; }
	public void setModelName(String modelName) { this.modelName = modelName; }

	public String getApiUrl() { return apiUrl; }
	public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }

	public String getJsonOverrides() { return jsonOverrides; }
	public void setJsonOverrides(String jsonOverrides) { this.jsonOverrides = jsonOverrides; }

	public boolean getUseStreaming() { return useStreaming; }
	public void setUseStreaming(boolean useStreaming) { this.useStreaming = useStreaming; }

	public boolean getUseSystemMessage() { return useSystemMessage; }
	public void setUseSystemMessage(boolean useSystemMessage) { this.useSystemMessage = useSystemMessage; }

	public boolean getUseDeveloperMessage() { return useDeveloperMessage; }
	public void setUseDeveloperMessage(boolean useDeveloperMessage) { this.useDeveloperMessage = useDeveloperMessage; }

	@Override
	public int compareTo(BookmarkedApiSettings other) {
		// Use URL as the primary key to make it easier to see the different "blocks" of models.
		int urlCompare = this.apiUrl.compareTo(other.apiUrl);
		if (urlCompare != 0) return urlCompare;

		int nameCompare = this.modelName.compareTo(other.modelName);
		if (nameCompare != 0) return nameCompare;

		int keyCompare = this.apiKey.compareTo(other.apiKey);
		if (keyCompare != 0) return keyCompare;

		int jsonOverridesCompare = this.jsonOverrides.compareTo(other.jsonOverrides);
		if (jsonOverridesCompare != 0) return jsonOverridesCompare;

		int useStreamingCompare = Boolean.compare(this.useStreaming, other.useStreaming);
		if (useStreamingCompare != 0) return useStreamingCompare;

		int useSystemMessageCompare = Boolean.compare(this.useSystemMessage, other.useSystemMessage);
		if (useSystemMessageCompare != 0) return useSystemMessageCompare;

		int useDeveloperMessageCompare = Boolean.compare(this.useDeveloperMessage, other.useDeveloperMessage);
		if (useDeveloperMessageCompare != 0) return useDeveloperMessageCompare;

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		BookmarkedApiSettings other = (BookmarkedApiSettings) obj;
		return Objects.equals(modelName, other.modelName)
				&& Objects.equals(apiUrl, other.apiUrl)
				&& Objects.equals(apiKey, other.apiKey)
				&& Objects.equals(jsonOverrides, other.jsonOverrides)
				&& (Boolean.compare(useStreaming, other.useStreaming) == 0)
				&& (Boolean.compare(useSystemMessage, other.useSystemMessage) == 0)
				&& (Boolean.compare(useDeveloperMessage, other.useDeveloperMessage) == 0);
	}

	@Override
	public int hashCode() {
		return Objects.hash(modelName, apiUrl, apiKey, jsonOverrides, useStreaming, useSystemMessage, useDeveloperMessage);
	}

}