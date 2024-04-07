package eclipse.plugin.aiassistant.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import eclipse.plugin.aiassistant.Logger;
import eclipse.plugin.aiassistant.preferences.Preferences;

public class HttpClientWrapper {

	private final HttpClient client;

	/**
	 * Constructor for HttpClientWrapper. Initializes the logger and creates a new
	 * HttpClient instance with a connection timeout duration set to the value
	 * returned by Activator.getConnectionTimeout().
	 */
	public HttpClientWrapper() {
		this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Preferences.getConnectionTimeout()))
				.build();
	}

	/**
	 * Checks if an address is reachable by sending a GET request and checking the
	 * response status code.
	 * 
	 * @param uri The URI of the address to check.
	 * @return True if the address is reachable, false otherwise.
	 */
	public Boolean isAddressReachable(URI uri) {
		try {
			HttpRequest request = buildRequest(uri, null);
			var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			return response.statusCode() == 200;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Sends an HTTP request with a specified URI and body.
	 * 
	 * @param uri  The URI of the request.
	 * @param body The body of the request, or null if no body is required.
	 * @return The HttpResponse object containing the response data.
	 * @throws IOException If an error occurs while sending the request.
	 */
	public HttpResponse<InputStream> sendRequest(URI uri, String body) throws IOException {
		try {
			HttpRequest request = buildRequest(uri, body);
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
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
	 * Sends an asynchronous HTTP request with a specified URI and body.
	 * 
	 * @param uri  The URI of the request.
	 * @param body The body of the request, or null if no body is required.
	 */
	public void sendRequestAsync(URI uri, String body) {
		try {
			HttpRequest request = buildRequest(uri, body);
			client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
		} catch (Exception e) {
			Logger.error("Failed to send async request", e);
		}
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
					.header("Accept", "text/event-stream").header("Content-Type", "application/json");
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