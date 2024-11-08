package eclipse.plugin.aiassistant.preferences;

import java.io.Serializable;
import java.util.Objects;

public class BookmarkedApiSettings implements Serializable, Comparable<BookmarkedApiSettings> {
    private static final long serialVersionUID = 1L;
    
    private String modelName;
    private String apiUrl;
    private String apiKey;
    private double temperature;
    
    public BookmarkedApiSettings(String modelName, String apiUrl, String apiKey, double temperature) {
        this.modelName = modelName;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.temperature = temperature;
    }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    @Override
    public int compareTo(BookmarkedApiSettings other) {
        int nameCompare = this.modelName.compareTo(other.modelName);
        if (nameCompare != 0) return nameCompare;
        
        int urlCompare = this.apiUrl.compareTo(other.apiUrl);
        if (urlCompare != 0) return urlCompare;
        
        int keyCompare = this.apiKey.compareTo(other.apiKey);
        if (keyCompare != 0) return keyCompare;
        
        return Double.compare(this.temperature, other.temperature);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BookmarkedApiSettings other = (BookmarkedApiSettings) obj;
        return Double.compare(temperature, other.temperature) == 0
            && Objects.equals(modelName, other.modelName)
            && Objects.equals(apiUrl, other.apiUrl)
            && Objects.equals(apiKey, other.apiKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, apiUrl, apiKey, temperature);
    }

}
