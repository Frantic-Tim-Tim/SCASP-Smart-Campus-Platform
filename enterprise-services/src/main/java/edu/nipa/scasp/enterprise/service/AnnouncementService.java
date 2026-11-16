package edu.nipa.scasp.enterprise.service;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;

import java.util.List;
import java.util.UUID;

public interface AnnouncementService {

    Announcement publish(
            String actorUserId,
            String title,
            String body,
            AnnouncementAudience audience
    );

    Announcement revise(
            String actorUserId,
            UUID announcementId,
            String title,
            String body,
            AnnouncementAudience audience
    );

    Announcement archive(String actorUserId, UUID announcementId);

    List<Announcement> getAll(String actorUserId);

    List<AnnouncementNotification> getForUser(String userId);

    void markRead(String userId, UUID announcementId);
}
