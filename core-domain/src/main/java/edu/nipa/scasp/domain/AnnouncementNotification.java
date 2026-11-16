package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;

public record AnnouncementNotification(
        Announcement announcement,
        boolean read,
        Instant readAt
) {
    public AnnouncementNotification {
        Objects.requireNonNull(announcement, "announcement");
        if (read && readAt == null) {
            throw new IllegalArgumentException(
                    "readAt is required when an announcement is read"
            );
        }
        if (!read && readAt != null) {
            throw new IllegalArgumentException(
                    "readAt must be null when an announcement is unread"
            );
        }
    }
}
