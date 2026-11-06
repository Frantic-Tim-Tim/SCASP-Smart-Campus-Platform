package edu.nipa.scasp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UdpAnnouncementListener implements AutoCloseable {

    private final DatagramSocket socket;

    public UdpAnnouncementListener(int port) throws IOException {

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }

        socket = new DatagramSocket(port);
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public String receiveMessage(int timeoutMillis) throws IOException {

        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        socket.setSoTimeout(timeoutMillis);

        byte[] buffer = new byte[65507];

        DatagramPacket packet = new DatagramPacket(
                buffer,
                buffer.length
        );

        try {
            socket.receive(packet);
        } catch (SocketTimeoutException exception) {
            return null;
        }

        return new String(
                packet.getData(),
                packet.getOffset(),
                packet.getLength(),
                StandardCharsets.UTF_8
        );
    }

    @Override
    public void close() {
        socket.close();
    }
}