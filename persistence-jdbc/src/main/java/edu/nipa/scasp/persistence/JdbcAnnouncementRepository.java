package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcAnnouncementRepository
        implements edu.nipa.scasp.domain.Repository<UUID, Announcement> {
    private final DataSource dataSource;

    public JdbcAnnouncementRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Announcement> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM announcements WHERE announcement_id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find announcement", failure);
        }
    }

    @Override
    public List<Announcement> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM announcements ORDER BY published_at DESC");
             ResultSet results = statement.executeQuery()) {
            List<Announcement> announcements = new ArrayList<>();
            while (results.next()) {
                announcements.add(read(results));
            }
            return List.copyOf(announcements);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list announcements", failure);
        }
    }

    @Override
    public void save(UUID id, Announcement announcement) {
        if (!Objects.requireNonNull(id, "id").equals(announcement.announcementId())) {
            throw new IllegalArgumentException("Repository key must match announcementId");
        }
        String sql = "UPDATE announcements SET title = ?, body = ?, audience = ?, published_at = ? "
                + "WHERE announcement_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            bind(update, announcement, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO announcements (title, body, audience, published_at, "
                                + "announcement_id) VALUES (?, ?, ?, ?, ?)")) {
                    bind(insert, announcement, id);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save announcement", failure);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM announcements WHERE announcement_id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete announcement", failure);
        }
    }

    private static void bind(PreparedStatement statement, Announcement announcement, UUID id)
            throws SQLException {
        statement.setString(1, announcement.title());
        statement.setString(2, announcement.body());
        statement.setString(3, announcement.audience().name());
        statement.setTimestamp(4, JdbcSupport.timestamp(announcement.publishedAt()));
        statement.setString(5, id.toString());
    }

    private static Announcement read(ResultSet results) throws SQLException {
        return new Announcement(UUID.fromString(results.getString("announcement_id")),
                results.getString("title"), results.getString("body"),
                AnnouncementAudience.valueOf(results.getString("audience")),
                JdbcSupport.instant(results.getTimestamp("published_at")));
    }
}
