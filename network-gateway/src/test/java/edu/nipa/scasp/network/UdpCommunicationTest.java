package edu.nipa.scasp.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UdpCommunicationTest {

    @Test
    void shouldSendAndReceiveAnnouncement() throws Exception {

        try (UdpAnnouncementListener listener =
                     new UdpAnnouncementListener(0)) {

            UdpAnnouncementBroadcaster broadcaster =
                    new UdpAnnouncementBroadcaster();

            broadcaster.sendMessage(
                    "localhost",
                    listener.getPort(),
                    "ANNOUNCEMENT: Registration closes on Friday."
            );

            String received = listener.receiveMessage(3000);

            assertEquals(
                    "ANNOUNCEMENT: Registration closes on Friday.",
                    received
            );
        }
    }

    @Test
    void shouldSendAndReceiveHeartbeat() throws Exception {

        try (UdpAnnouncementListener listener =
                     new UdpAnnouncementListener(0)) {

            UdpAnnouncementBroadcaster broadcaster =
                    new UdpAnnouncementBroadcaster();

            broadcaster.sendMessage(
                    "localhost",
                    listener.getPort(),
                    "HEARTBEAT: SCASP server is active."
            );

            String received = listener.receiveMessage(3000);

            assertEquals(
                    "HEARTBEAT: SCASP server is active.",
                    received
            );
        }
    }

    @Test
    void shouldReturnNullWhenNoMessageArrives() throws Exception {

        try (UdpAnnouncementListener listener =
                     new UdpAnnouncementListener(0)) {

            String received = listener.receiveMessage(200);

            assertNull(received);
        }
    }
}