package eclipse.plugin.aiassistant.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * A wrapper around {@link HttpClient} that provides simplified HTTP communication
 * with a specific API endpoint. Handles authentication, request building, and
 * connection validation.
 * 
 * @see java.net.http.HttpClient
 */
public class HttpClientWrapper {

    private final URI apiUri;
    private final String apiKey;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    /**
     * Creates a new HTTP client wrapper and validates the connection.
     *
     * @param apiUri The API URI for all requests
     * @param apiKey The API key for authentication
     * @param connectionTimeout Timeout for establishing connections
     * @param requestTimeout Timeout for complete request/response cycle
     */
    public HttpClientWrapper(URI apiUri, String apiKey, Duration connectionTimeout, Duration requestTimeout) {
        this.apiUri = apiUri;
        this.apiKey = apiKey;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectionTimeout).build();
    }
    
    /**
     * Sends a synchronous HTTP request with the provided body.
     *
     * @param body The JSON request body (may be null for GET requests)
     * @return Response with input stream for reading the response body
     * @throws IOException If the request fails or returns a non-200 status code
     */
    public HttpResponse<InputStream> sendRequest(String body) throws IOException {
        HttpResponse<InputStream> response;
        HttpRequest request = buildRequest(body);
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IOException("Failed to send request", e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Request failed with status code " + response.statusCode());
        }
        return response;
    }

    /**
     * Sends an asynchronous HTTP request without waiting for the response.
     *
     * @param body The JSON request body
     * @throws IOException If request building fails
     */
    public void sendRequestAsync(String body) throws IOException {
        HttpRequest request = buildRequest(body);
        try {
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IOException("Failed to send async request", e);
        }
    }
    
    /**
     * Builds an HTTP request with standard headers and optional body.
     * Uses HTTP/1.1 and includes Authorization, Accept, and Content-Type headers.
     *
     * @param body Request body (null or empty for GET, non-empty for POST)
     * @return Built HTTP request
     * @throws IOException If request building fails
     */
    private HttpRequest buildRequest(String body) throws IOException {
        HttpRequest request;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            		.uri(apiUri)
            		.timeout(requestTimeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json");
            if (body == null || body.isEmpty()) {
                requestBuilder.GET();
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            }
            request = requestBuilder.build();
        } catch (Exception e) {
            throw new IOException("Could not build the request", e);
        }
        return request;
    }
    
}