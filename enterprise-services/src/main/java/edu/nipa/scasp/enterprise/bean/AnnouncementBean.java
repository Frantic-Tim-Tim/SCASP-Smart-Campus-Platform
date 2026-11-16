package edu.nipa.scasp.enterprise.bean;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.AnnouncementRepository;
import edu.nipa.scasp.enterprise.service.AnnouncementService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Stateless
@Local(AnnouncementService.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AnnouncementBean implements AnnouncementService {

    @EJB
    private AnnouncementRepository announcementRepository;

    @EJB
    private UserManagementService userManagementService;

    @Override
    public Announcement publish(
            String actorUserId,
            String title,
            String body,
            AnnouncementAudience audience
    ) {
        requireAdministrator(actorUserId);

        Announcement announcement = new Announcement(
                UUID.randomUUID(),
                requireText(title, "title"),
                requireText(body, "body"),
                Objects.requireNonNull(audience, "audience"),
                Instant.now()
        );

        announcementRepository.create(announcement);
        return announcement;
    }

    @Override
    public Announcement revise(
            String actorUserId,
            UUID announcementId,
            String title,
            String body,
            AnnouncementAudience audience
    ) {
        requireAdministrator(actorUserId);

        Announcement existing = findAnnouncement(announcementId);

        if (existing.archived()) {
            throw new IllegalStateException(
                    "Archived announcements cannot be edited"
            );
        }

        Announcement revised = existing.revise(
                requireText(title, "title"),
                requireText(body, "body"),
                Objects.requireNonNull(audience, "audience")
        );

        if (!announcementRepository.update(revised)) {
            throw new NoSuchElementException("Announcement no longer exists");
        }

        return revised;
    }

    @Override
    public Announcement archive(
            String actorUserId,
            UUID announcementId
    ) {
        requireAdministrator(actorUserId);

        Announcement existing = findAnnouncement(announcementId);
        if (existing.archived()) {
            return existing;
        }

        Announcement archived = existing.archive();

        if (!announcementRepository.update(archived)) {
            throw new NoSuchElementException("Announcement no longer exists");
        }

        return archived;
    }

    @Override
    public List<Announcement> getAll(String actorUserId) {
        requireAdministrator(actorUserId);
        return announcementRepository.findAll();
    }

    @Override
    public List<AnnouncementNotification> getForUser(String userId) {
        User user = requireActiveUser(userId);

        AnnouncementAudience audience = switch (user.role()) {
            case STUDENT -> AnnouncementAudience.STUDENTS;
            case STAFF -> AnnouncementAudience.STAFF;
            case ADMIN -> AnnouncementAudience.ALL;
        };

        return announcementRepository.findForUser(userId, audience);
    }

    @Override
    public void markRead(String userId, UUID announcementId) {
        User user = requireActiveUser(userId);

        Announcement announcement = findAnnouncement(announcementId);

        if (announcement.archived()) {
            throw new IllegalStateException(
                    "Archived announcements cannot be marked as read"
            );
        }

        boolean visible = announcement.audience() == AnnouncementAudience.ALL
                || (user.role() == UserRole.STUDENT
                    && announcement.audience() == AnnouncementAudience.STUDENTS)
                || (user.role() == UserRole.STAFF
                    && announcement.audience() == AnnouncementAudience.STAFF);

        if (!visible) {
            throw new SecurityException(
                    "You are not allowed to access this announcement"
            );
        }

        if (!announcementRepository.markRead(announcementId, userId)) {
            throw new IllegalStateException(
                    "The announcement could not be marked as read"
            );
        }
    }

    private User requireAdministrator(String userId) {
        User user = requireActiveUser(userId);

        if (user.role() != UserRole.ADMIN) {
            throw new SecurityException(
                    "Only administrators can manage announcements"
            );
        }

        return user;
    }

    private User requireActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("Authentication is required");
        }

        User user = userManagementService.getUserById(userId);

        if (user == null || user.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "Your account is not active or could not be found"
            );
        }

        return user;
    }

    private Announcement findAnnouncement(UUID announcementId) {
        Objects.requireNonNull(announcementId, "announcementId");

        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Announcement not found: " + announcementId
                ));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }
}
