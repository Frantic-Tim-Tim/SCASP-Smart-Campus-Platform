package edu.nipa.scasp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpAnnouncementBroadcaster {

    public void sendMessage(String host, int port, String message)
            throws IOException {

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be empty");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid destination port");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        if (data.length > 65507) {
            throw new IllegalArgumentException("UDP message is too large");
        }

        InetAddress address = InetAddress.getByName(host);

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                address,
                port
        );

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        }
    }
}