package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcAnnouncementRepository implements AnnouncementRepository {

    private final ConnectionFactory connectionFactory;

    public JdbcAnnouncementRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void create(Announcement announcement) throws SQLException {
        String sql = """
                INSERT INTO announcements (
                    announcement_id, title, body, audience,
                    published_at, archived
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, announcement.announcementId().toString());
            statement.setString(2, announcement.title());
            statement.setString(3, announcement.body());
            statement.setString(4, announcement.audience().name());
            statement.setTimestamp(
                    5, Timestamp.from(announcement.publishedAt()));
            statement.setBoolean(6, announcement.archived());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Announcement> findById(UUID announcementId)
            throws SQLException {
        String sql = """
                SELECT announcement_id, title, body, audience,
                       published_at, archived
                FROM announcements
                WHERE announcement_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, announcementId.toString());

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(mapAnnouncement(result))
                        : Optional.empty();
            }
        }
    }

    @Override
    public List<Announcement> findAll() throws SQLException {
        String sql = """
                SELECT announcement_id, title, body, audience,
                       published_at, archived
                FROM announcements
                ORDER BY published_at DESC
                """;

        List<Announcement> announcements = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                announcements.add(mapAnnouncement(result));
            }
        }

        return announcements;
    }

    @Override
    public List<AnnouncementNotification> findForUser(
            String userId,
            AnnouncementAudience audience
    ) throws SQLException {
        String sql = """
                SELECT a.announcement_id, a.title, a.body, a.audience,
                       a.published_at, a.archived,
                       r.read_at
                FROM announcements a
                LEFT JOIN announcement_reads r
                  ON r.announcement_id = a.announcement_id
                 AND r.user_id = ?
                WHERE a.archived = FALSE
                  AND (a.audience = 'ALL' OR a.audience = ?)
                ORDER BY a.published_at DESC
                """;

        List<AnnouncementNotification> notifications = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, audience.name());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Announcement announcement = mapAnnouncement(result);
                    Timestamp readTimestamp = result.getTimestamp("read_at");
                    Instant readAt = readTimestamp == null
                            ? null
                            : readTimestamp.toInstant();

                    notifications.add(new AnnouncementNotification(
                            announcement,
                            readAt != null,
                            readAt
                    ));
                }
            }
        }

        return notifications;
    }

    @Override
    public boolean update(Announcement announcement) throws SQLException {
        String sql = """
                UPDATE announcements
                SET title = ?, body = ?, audience = ?, archived = ?
                WHERE announcement_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, announcement.title());
            statement.setString(2, announcement.body());
            statement.setString(3, announcement.audience().name());
            statement.setBoolean(4, announcement.archived());
            statement.setString(5, announcement.announcementId().toString());

            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean markRead(UUID announcementId, String userId)
            throws SQLException {
        String updateSql = """
                UPDATE announcement_reads
                SET read_at = ?
                WHERE announcement_id = ? AND user_id = ?
                """;

        Instant now = Instant.now();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            update.setTimestamp(1, Timestamp.from(now));
            update.setString(2, announcementId.toString());
            update.setString(3, userId);

            if (update.executeUpdate() == 1) {
                return true;
            }
        }

        String insertSql = """
                INSERT INTO announcement_reads (
                    announcement_id, user_id, read_at
                )
                SELECT ?, ?, ?
                WHERE EXISTS (
                    SELECT 1 FROM announcements
                    WHERE announcement_id = ? AND archived = FALSE
                )
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, announcementId.toString());
            insert.setString(2, userId);
            insert.setTimestamp(3, Timestamp.from(now));
            insert.setString(4, announcementId.toString());

            return insert.executeUpdate() == 1;
        }
    }

    private Announcement mapAnnouncement(ResultSet result)
            throws SQLException {
        return new Announcement(
                UUID.fromString(result.getString("announcement_id")),
                result.getString("title"),
                result.getString("body"),
                AnnouncementAudience.valueOf(result.getString("audience")),
                result.getTimestamp("published_at").toInstant(),
                result.getBoolean("archived")
        );
    }
}
