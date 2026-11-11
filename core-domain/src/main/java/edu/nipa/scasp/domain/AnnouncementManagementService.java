package edu.nipa.scasp.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AnnouncementManagementService {
    private final Map<UUID, Announcement> announcements = new HashMap<>();
    private final AuditLog auditLog;

    public AnnouncementManagementService() {
        this(new AuditLog());
    }

    public AnnouncementManagementService(AuditLog auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    public synchronized void publish(User actor, Announcement announcement) {
        requireStaff(actor);
        Objects.requireNonNull(announcement, "announcement");
        if (announcements.putIfAbsent(announcement.announcementId(), announcement) != null) {
            throw new IllegalArgumentException("Announcement already exists");
        }
        auditLog.record(actor.userId(), "ANNOUNCEMENT_PUBLISHED", "Announcement",
                announcement.announcementId().toString(), announcement.publishedAt());
    }

    public synchronized List<Announcement> forAudience(User audience) {
        Objects.requireNonNull(audience, "audience");
        if (audience.status() != UserStatus.ACTIVE) {
            throw new SecurityException("An active user is required");
        }
        boolean student = audience.role() == UserRole.STUDENT;
        return announcements.values().stream()
                .filter(item -> item.audience() == AnnouncementAudience.ALL
                        || (student && item.audience() == AnnouncementAudience.STUDENTS)
                        || (!student && item.audience() == AnnouncementAudience.STAFF))
                .sorted(Comparator.comparing(Announcement::publishedAt).reversed())
                .toList();
    }

    public List<AuditEvent> auditEvents() {
        return auditLog.events();
    }

    private static void requireStaff(User actor) {
        if (actor == null || actor.status() != UserStatus.ACTIVE
                || actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
    }
}
