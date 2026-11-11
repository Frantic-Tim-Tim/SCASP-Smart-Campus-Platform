package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.WaitlistEntry;
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

public final class JdbcWaitlistRepository
        implements edu.nipa.scasp.domain.Repository<UUID, WaitlistEntry> {
    private final DataSource dataSource;

    public JdbcWaitlistRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<WaitlistEntry> findById(UUID id) {
        String sql = "SELECT entry_id, student_id, course_code, position, created_at "
                + "FROM waitlist_entries WHERE entry_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find waitlist entry", failure);
        }
    }

    @Override
    public List<WaitlistEntry> findAll() {
        String sql = "SELECT entry_id, student_id, course_code, position, created_at "
                + "FROM waitlist_entries ORDER BY course_code, position";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<WaitlistEntry> entries = new ArrayList<>();
            while (results.next()) {
                entries.add(read(results));
            }
            return List.copyOf(entries);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list waitlist entries", failure);
        }
    }

    @Override
    public void save(UUID id, WaitlistEntry entry) {
        if (!Objects.requireNonNull(id, "id").equals(entry.entryId())) {
            throw new IllegalArgumentException("Repository key must match entryId");
        }
        String sql = "UPDATE waitlist_entries SET student_id = ?, course_code = ?, position = ?, "
                + "created_at = ? WHERE entry_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            bind(update, entry, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO waitlist_entries (student_id, course_code, position, "
                                + "created_at, entry_id) VALUES (?, ?, ?, ?, ?)")) {
                    bind(insert, entry, id);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save waitlist entry", failure);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM waitlist_entries WHERE entry_id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete waitlist entry", failure);
        }
    }

    private static void bind(PreparedStatement statement, WaitlistEntry entry, UUID id)
            throws SQLException {
        statement.setString(1, entry.studentId());
        statement.setString(2, entry.courseCode());
        statement.setInt(3, entry.position());
        statement.setTimestamp(4, JdbcSupport.timestamp(entry.createdAt()));
        statement.setString(5, id.toString());
    }

    private static WaitlistEntry read(ResultSet result) throws SQLException {
        return new WaitlistEntry(UUID.fromString(result.getString("entry_id")),
                result.getString("student_id"), result.getString("course_code"),
                result.getInt("position"), JdbcSupport.instant(result.getTimestamp("created_at")));
    }
}
