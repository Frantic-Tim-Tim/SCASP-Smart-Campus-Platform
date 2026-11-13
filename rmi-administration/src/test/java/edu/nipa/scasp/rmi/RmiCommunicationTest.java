package edu.nipa.scasp.rmi;

import edu.nipa.scasp.rmi.client.RmiClient;
import edu.nipa.scasp.rmi.dto.ServerStatus;
import edu.nipa.scasp.rmi.server.RmiServer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;

class RmiCommunicationTest {

    @Test
    void clientShouldCommunicateWithRemoteServer()
            throws IOException, NotBoundException {

        int registryPort = findAvailablePort();
        int servicePort = findAvailablePort();

        RmiServer server = new RmiServer(
                registryPort,
                servicePort
        );

        try {
            // Start the RMI server.
            server.start();

            // Connect the client to the running server.
            RmiClient client = new RmiClient(
                    "localhost",
                    registryPort
            );

            // Test remote ping operation.
            assertEquals(
                    "SCASP RMI Server is running!",
                    client.ping()
            );

            // Test remote server status operation.
            ServerStatus status = client.getServerStatus();

            assertNotNull(status);
            assertEquals(
                    "SCASP Remote Administration Server",
                    status.serverName()
            );
            assertEquals("RUNNING", status.status());
            assertNotNull(status.checkedAt());

        } finally {
            // Always release server resources.
            server.close();
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
