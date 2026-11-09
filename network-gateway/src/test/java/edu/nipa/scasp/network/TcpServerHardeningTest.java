package edu.nipa.scasp.network;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TcpServerHardeningTest {

    @Test
    void shouldRejectClientsWhenWorkerQueueIsFull()
            throws Exception {

        // One worker and one queue slot.
        try (TcpServer server = new TcpServer(0, 1, 1)) {

            server.start();

            try (
                    Socket firstClient =
                            new Socket("localhost", server.getPort());

                    Socket secondClient =
                            new Socket("localhost", server.getPort());

                    Socket rejectedClient =
                            new Socket("localhost", server.getPort())
            ) {

                rejectedClient.setSoTimeout(3000);

                // The first client occupies the only worker.
                awaitCondition(
                        () -> server.getActiveWorkerCount() == 1,
                        3000,
                        "First client did not occupy the worker"
                );

                // The second client occupies the only queue slot.
                awaitCondition(
                        () -> server.getQueuedClientCount() == 1,
                        3000,
                        "Second client was not queued"
                );

                // The third client should be rejected and disconnected.
                int result =
                        rejectedClient.getInputStream().read();

                assertEquals(
                        -1,
                        result,
                        "Server should close a client when its queue is full"
                );

                assertEquals(
                        2,
                        server.getConnectedClientCount(),
                        "Only the active and queued clients should remain"
                );
            }
        }
    }

    @Test
    void shouldCloseConnectionWhenCommandExceedsMaximumLength()
            throws Exception {

        try (TcpServer server = new TcpServer(0, 1)) {

            server.start();

            try (Socket client =
                         new Socket("localhost", server.getPort())) {

                client.setSoTimeout(3000);

                // The maximum accepted command length is 1024 characters.
                String oversizedCommand = "A".repeat(1025) + "\n";

                OutputStream output = client.getOutputStream();

                output.write(
                        oversizedCommand.getBytes(StandardCharsets.UTF_8)
                );

                output.flush();

                int result = client.getInputStream().read();

                assertEquals(
                        -1,
                        result,
                        "Server should close connections containing oversized commands"
                );
            }
        }
    }

    @Test
    void shouldPreventRestartAfterServerHasBeenClosed()
            throws Exception {

        TcpServer server = new TcpServer(0, 1);

        server.start();

        server.close();

        assertThrows(
                IllegalStateException.class,
                server::start,
                "A closed server instance should not be restartable"
        );
    }

    @Test
    void shouldRejectStartingAnAlreadyRunningServer()
            throws Exception {

        try (TcpServer server = new TcpServer(0, 1)) {

            server.start();

            assertThrows(
                    IllegalStateException.class,
                    server::start,
                    "A server that is already running should not start again"
            );
        }
    }

    /**
     * Waits for an asynchronous server condition to become true.
     *
     * This avoids relying on arbitrary delays when testing
     * concurrent client connections.
     */
    private void awaitCondition(
            BooleanSupplier condition,
            long timeoutMillis,
            String failureMessage
    ) throws InterruptedException {

        long deadline =
                System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {

            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(20);
        }

        assertTrue(condition.getAsBoolean(), failureMessage);
    }
}