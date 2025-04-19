package eclipse.plugin.aiassistant.preferences;

import java.io.Serializable;
import java.util.Objects;

public class BookmarkedApiSettings implements Serializable, Comparable<BookmarkedApiSettings> {
	private static final long serialVersionUID = 1L;

	private String modelName;
	private String apiUrl;
	private String apiKey;
	private double temperature;
	private boolean useSystemMessage;
	private boolean useStreaming;

	public BookmarkedApiSettings(String modelName, String apiUrl, String apiKey, double temperature,
			boolean useSystemMessage, boolean useStreaming) {
		this.modelName = modelName;
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.temperature = temperature;
		this.useSystemMessage = useSystemMessage;
		this.useStreaming = useStreaming;
	}

	public String getModelName() { return modelName; }
	public void setModelName(String modelName) { this.modelName = modelName; }

	public String getApiUrl() { return apiUrl; }
	public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }

	public double getTemperature() { return temperature; }
	public void setTemperature(double temperature) { this.temperature = temperature; }

	public boolean getUseSystemMessage() { return useSystemMessage; }
	public void setUseSystemMessage(boolean useSystemMessage) { this.useSystemMessage = useSystemMessage; }

	public boolean getUseStreaming() { return useStreaming; }
	public void setUseStreaming(boolean useStreaming) { this.useStreaming = useStreaming; }

	@Override
	public int compareTo(BookmarkedApiSettings other) {
		// Use URL as the primary key to make it easier to see the different "blocks" of models.
		int urlCompare = this.apiUrl.compareTo(other.apiUrl);
		if (urlCompare != 0) return urlCompare;

		int nameCompare = this.modelName.compareTo(other.modelName);
		if (nameCompare != 0) return nameCompare;

		int keyCompare = this.apiKey.compareTo(other.apiKey);
		if (keyCompare != 0) return keyCompare;

		int temperatureCompare = Double.compare(this.temperature, other.temperature);
		if (temperatureCompare != 0) return temperatureCompare;

		int useSystemMessageCompare = Boolean.compare(this.useSystemMessage, other.useSystemMessage);
		if (useSystemMessageCompare != 0) return useSystemMessageCompare;

		int useStreamingCompare = Boolean.compare(this.useStreaming, other.useStreaming);
		if (useStreamingCompare != 0) return useStreamingCompare;

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
				&& (Double.compare(temperature, other.temperature) == 0)
				&& (Boolean.compare(useSystemMessage, other.useSystemMessage) == 0)
				&& (Boolean.compare(useStreaming, other.useStreaming) == 0);
	}

	@Override
	public int hashCode() {
		return Objects.hash(modelName, apiUrl, apiKey, temperature, useSystemMessage, useStreaming);
	}

}
