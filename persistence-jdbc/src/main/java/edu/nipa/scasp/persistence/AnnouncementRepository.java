package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {

    void create(Announcement announcement) throws SQLException;

    Optional<Announcement> findById(UUID announcementId)
            throws SQLException;

    List<Announcement> findAll() throws SQLException;

    List<AnnouncementNotification> findForUser(
            String userId,
            AnnouncementAudience audience
    ) throws SQLException;

    boolean update(Announcement announcement) throws SQLException;

    boolean markRead(UUID announcementId, String userId)
            throws SQLException;
}
