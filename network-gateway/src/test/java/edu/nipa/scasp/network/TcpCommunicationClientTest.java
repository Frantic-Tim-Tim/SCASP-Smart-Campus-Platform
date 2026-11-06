package edu.nipa.scasp.network;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TcpConcurrentClientTest {

    @Test
    void shouldHandleMultipleClientsAtTheSameTime() throws Exception {

        try (TcpServer server = new TcpServer(0, 4);
             ExecutorService executor = Executors.newFixedThreadPool(4)) {

            server.start();

            List<Future<String>> results = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                results.add(executor.submit(() -> {
                    try (TcpClient client =
                                 new TcpClient("localhost", server.getPort())) {

                        return client.sendCommand("GET_COURSES");
                    }
                }));
            }

            for (Future<String> result : results) {
                assertEquals(
                    "SUCCESS: Course catalogue requested",
                    result.get(10, TimeUnit.SECONDS)
                );
            }
        }
    }
}