package edu.nipa.scasp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UdpAnnouncementPublisherTest {
    @Test
    void publishesAnnouncementToConfiguredLoopbackReceiver() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (DatagramSocket receiver = new DatagramSocket(0, loopback)) {
            receiver.setSoTimeout(1000);
            new UdpAnnouncementPublisher().publish("Campus notice", loopback,
                    receiver.getLocalPort());
            byte[] buffer = new byte[128];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            receiver.receive(packet);

            assertEquals("Campus notice", new String(packet.getData(), packet.getOffset(),
                    packet.getLength(), StandardCharsets.UTF_8));
        }
    }
}
