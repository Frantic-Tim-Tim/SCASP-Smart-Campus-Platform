package edu.nipa.scasp.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AuthorizedResourceClient {
    private final HttpClient client;
    private final Set<String> allowedHosts;
    private final Duration requestTimeout;

    public AuthorizedResourceClient(Set<String> allowedHosts, Duration requestTimeout) {
        this.allowedHosts = Objects.requireNonNull(allowedHosts, "allowedHosts").stream()
                .map(host -> host.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        client = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String get(URI uri) throws IOException, InterruptedException {
        validate(uri);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(requestTimeout).GET().build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Authorized resource returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void validate(URI uri) {
        Objects.requireNonNull(uri, "uri");
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !allowedHosts.contains(uri.getHost().toLowerCase(Locale.ROOT))
                || uri.getUserInfo() != null) {
            throw new SecurityException("URI is not an authorized HTTPS resource");
        }
    }
}
