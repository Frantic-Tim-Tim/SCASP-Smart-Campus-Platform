package edu.nipa.scasp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class UdpAnnouncementPublisher {
    private static final int MAX_DATAGRAM_BYTES = 1200;

    public void publish(String message, InetAddress address, int port) throws IOException {
        byte[] content = validateMessage(message);
        Objects.requireNonNull(address, "address");
        validatePort(port);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(content, content.length, address, port));
        }
    }

    public String receive(SocketAddress bindAddress, int timeoutMillis) throws IOException {
        Objects.requireNonNull(bindAddress, "bindAddress");
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        byte[] buffer = new byte[MAX_DATAGRAM_BYTES + 1];
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.bind(bindAddress);
            socket.setSoTimeout(timeoutMillis);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            if (packet.getLength() > MAX_DATAGRAM_BYTES) {
                throw new IOException("Announcement datagram exceeds the size limit");
            }
            return new String(packet.getData(), packet.getOffset(), packet.getLength(),
                    StandardCharsets.UTF_8);
        } catch (SocketTimeoutException timeout) {
            throw timeout;
        }
    }

    private static byte[] validateMessage(String message) {
        Objects.requireNonNull(message, "message");
        byte[] content = message.getBytes(StandardCharsets.UTF_8);
        if (content.length == 0 || content.length > MAX_DATAGRAM_BYTES) {
            throw new IllegalArgumentException("Message must be 1 to 1200 UTF-8 bytes");
        }
        return content;
    }

    private static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
