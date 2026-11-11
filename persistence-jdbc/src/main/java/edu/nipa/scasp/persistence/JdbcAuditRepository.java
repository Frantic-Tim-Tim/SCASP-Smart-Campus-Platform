package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.AuditEvent;
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

public final class JdbcAuditRepository
        implements edu.nipa.scasp.domain.Repository<UUID, AuditEvent> {
    private final DataSource dataSource;

    public JdbcAuditRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM audit_events WHERE event_id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find audit event", failure);
        }
    }

    @Override
    public List<AuditEvent> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM audit_events ORDER BY occurred_at, event_id");
             ResultSet results = statement.executeQuery()) {
            List<AuditEvent> events = new ArrayList<>();
            while (results.next()) {
                events.add(read(results));
            }
            return List.copyOf(events);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list audit events", failure);
        }
    }

    @Override
    public void save(UUID id, AuditEvent event) {
        if (!Objects.requireNonNull(id, "id").equals(event.eventId())) {
            throw new IllegalArgumentException("Repository key must match eventId");
        }
        String sql = "INSERT INTO audit_events (event_id, actor, action, entity_type, entity_id, "
                + "occurred_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, event.actor());
            statement.setString(3, event.action());
            statement.setString(4, event.entityType());
            statement.setString(5, event.entityId());
            statement.setTimestamp(6, JdbcSupport.timestamp(event.occurredAt()));
            statement.executeUpdate();
        } catch (SQLException failure) {
            throw JdbcSupport.failure("record audit event", failure);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        throw new UnsupportedOperationException("Audit events are append-only");
    }

    private static AuditEvent read(ResultSet results) throws SQLException {
        return new AuditEvent(UUID.fromString(results.getString("event_id")),
                results.getString("actor"), results.getString("action"),
                results.getString("entity_type"), results.getString("entity_id"),
                JdbcSupport.instant(results.getTimestamp("occurred_at")));
    }
}
