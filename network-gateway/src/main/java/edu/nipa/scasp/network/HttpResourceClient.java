package edu.nipa.scasp.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpResourceClient {

    private final HttpClient httpClient;

    public HttpResourceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String getResource(String url) throws IOException,
            InterruptedException {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid URL", exception);
        }

        String scheme = uri.getScheme();

        if (scheme == null ||
                (!scheme.equalsIgnoreCase("http")
                && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are supported"
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {
            throw new IOException(
                    "HTTP request failed with status: "
                    + response.statusCode()
            );
        }

        return response.body();
    }
}