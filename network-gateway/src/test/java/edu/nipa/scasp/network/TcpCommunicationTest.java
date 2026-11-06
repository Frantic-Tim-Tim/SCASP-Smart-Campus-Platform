package edu.nipa.scasp.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TcpCommunicationTest {

    @Test
    void shouldSendCommandAndReceiveServerResponse()
            throws Exception {

        // Port 0 asks the operating system for an available port.
        try (TcpServer server = new TcpServer(0, 2)) {

            server.start();

            try (TcpClient client =
                         new TcpClient("localhost", server.getPort())) {

                String response =
                        client.sendCommand("GET_COURSES");

                assertEquals(
                        "SUCCESS: Course catalogue requested",
                        response
                );
            }
        }
    }

    @Test
    void shouldHandleMultipleCommandsOnOneConnection()
            throws Exception {

        try (TcpServer server = new TcpServer(0, 2)) {

            server.start();

            try (TcpClient client =
                         new TcpClient("localhost", server.getPort())) {

                assertEquals(
                        "SUCCESS: Course catalogue requested",
                        client.sendCommand("GET_COURSES")
                );

                assertEquals(
                        "SUCCESS: Announcements requested",
                        client.sendCommand("GET_ANNOUNCEMENTS")
                );
            }
        }
    }
}