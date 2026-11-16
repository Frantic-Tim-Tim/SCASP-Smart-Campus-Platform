package edu.nipa.scasp.enterprise.repository;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {
    void create(Announcement announcement);
    Optional<Announcement> findById(UUID announcementId);
    List<Announcement> findAll();

    List<AnnouncementNotification> findForUser(
            String userId,
            AnnouncementAudience audience
    );

    boolean update(Announcement announcement);
    boolean markRead(UUID announcementId, String userId);
}
