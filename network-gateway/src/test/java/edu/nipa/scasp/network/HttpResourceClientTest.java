package edu.nipa.scasp.network;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpResourceClientTest {

    @Test
    void shouldRetrieveResourceUsingHttpGet() throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0
        );

        server.createContext("/courses", exchange -> {
            String response = "SCASP Course Catalogue";

            byte[] data = response.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200, data.length);

            try (var output = exchange.getResponseBody()) {
                output.write(data);
            }
        });

        server.start();

        try {
            HttpResourceClient client = new HttpResourceClient();

            String url = "http://localhost:"
                    + server.getAddress().getPort()
                    + "/courses";

            String result = client.getResource(url);

            assertEquals("SCASP Course Catalogue", result);

        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectUnsupportedUrlScheme() {

        HttpResourceClient client = new HttpResourceClient();

        assertThrows(
                IllegalArgumentException.class,
                () -> client.getResource("ftp://localhost/courses")
        );
    }

    @Test
    void shouldRejectEmptyUrl() {

        HttpResourceClient client = new HttpResourceClient();

        assertThrows(
                IllegalArgumentException.class,
                () -> client.getResource(" ")
        );
    }
}