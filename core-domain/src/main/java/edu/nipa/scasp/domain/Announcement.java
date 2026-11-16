package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Announcement(
        UUID announcementId,
        String title,
        String body,
        AnnouncementAudience audience,
        Instant publishedAt,
        boolean archived
) {
    public Announcement(
            UUID announcementId,
            String title,
            String body,
            AnnouncementAudience audience,
            Instant publishedAt
    ) {
        this(announcementId, title, body, audience, publishedAt, false);
    }

    public Announcement {
        Objects.requireNonNull(announcementId, "announcementId");
        title = User.requireText(title, "title");
        body = User.requireText(body, "body");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(publishedAt, "publishedAt");
    }

    public Announcement revise(
            String newTitle,
            String newBody,
            AnnouncementAudience newAudience
    ) {
        return new Announcement(
                announcementId,
                newTitle,
                newBody,
                newAudience,
                publishedAt,
                archived
        );
    }

    public Announcement archive() {
        return new Announcement(
                announcementId,
                title,
                body,
                audience,
                publishedAt,
                true
        );
    }
}
