package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Announcement(UUID announcementId, String title, String body,
                           AnnouncementAudience audience, Instant publishedAt) {
    public Announcement {
        Objects.requireNonNull(announcementId, "announcementId");
        title = User.requireText(title, "title");
        body = User.requireText(body, "body");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(publishedAt, "publishedAt");
    }
}