package edu.nipa.scasp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nipa.scasp.domain.InMemoryCampusBackend;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TcpCampusServerTest {
    @Test
    void authenticatesAndServesRequestsThenShutsDown() throws Exception {
        InMemoryCampusBackend backend = new InMemoryCampusBackend();
        backend.addUser(new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE),
                "Correct-Horse-Battery!".toCharArray());
        TcpCampusServer server = new TcpCampusServer(InetAddress.getLoopbackAddress(), 0, 5000,
                backend, Clock.systemUTC());
        server.start();
        try (TcpCampusClient client = new TcpCampusClient("127.0.0.1", server.port(), 5000)) {
            assertEquals("OK|AUTHENTICATED",
                    client.authenticate("staff-1", "Correct-Horse-Battery!".toCharArray()));
            assertEquals("OK|COURSES|0", client.request("GET_COURSES"));
        } finally {
            server.close();
        }
        assertTrue(serverClosed(server));
    }

    @Test
    void idleClientReceivesTimeoutAndDoesNotPreventServerShutdown() throws Exception {
        TcpCampusServer server = new TcpCampusServer(InetAddress.getLoopbackAddress(), 0, 100,
                new InMemoryCampusBackend(), Clock.systemUTC());
        server.start();
        try (Socket socket = new Socket("127.0.0.1", server.port());
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(),
                     StandardCharsets.UTF_8)) {
            assertTrue(reader.readLine().startsWith("ERR|TIMEOUT|"));
        } finally {
            server.close();
        }
        assertTrue(serverClosed(server));
    }

    @Test
    void disconnectedClientDoesNotStopOtherConcurrentClients() throws Exception {
        InMemoryCampusBackend backend = new InMemoryCampusBackend();
        backend.addUser(new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE),
                "Correct-Horse-Battery!".toCharArray());
        TcpCampusServer server = new TcpCampusServer(InetAddress.getLoopbackAddress(), 0, 5000,
                backend, Clock.systemUTC());
        server.start();
        try {
            try (Socket disconnected = new Socket("127.0.0.1", server.port())) {
                disconnected.shutdownOutput();
            }
            try (TcpCampusClient clientA = new TcpCampusClient("127.0.0.1", server.port(), 5000);
                 TcpCampusClient clientB = new TcpCampusClient("127.0.0.1", server.port(), 5000)) {
                assertEquals("OK|AUTHENTICATED",
                        clientA.authenticate("staff-1", "Correct-Horse-Battery!".toCharArray()));
                assertEquals("OK|AUTHENTICATED",
                        clientB.authenticate("staff-1", "Correct-Horse-Battery!".toCharArray()));
                assertEquals("OK|COURSES|0", clientA.request("GET_COURSES"));
                assertEquals("OK|COURSES|0", clientB.request("GET_COURSES"));
            }
        } finally {
            server.close();
        }
    }

    private static boolean serverClosed(TcpCampusServer server) throws Exception {
        return serverPortUnavailable(server);
    }

    private static boolean serverPortUnavailable(TcpCampusServer server) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", server.port()),
                    (int) TimeUnit.SECONDS.toMillis(1));
            return false;
        } catch (java.net.ConnectException expected) {
            return true;
        }
    }
}
