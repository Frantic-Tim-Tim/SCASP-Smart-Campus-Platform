package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.domain.ServiceRequest;
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

public final class JdbcServiceRequestRepository
        implements edu.nipa.scasp.domain.Repository<UUID, ServiceRequest> {
    private final DataSource dataSource;

    public JdbcServiceRequestRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<ServiceRequest> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM service_requests WHERE request_id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find service request", failure);
        }
    }

    @Override
    public List<ServiceRequest> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM service_requests ORDER BY created_at, request_id");
             ResultSet results = statement.executeQuery()) {
            List<ServiceRequest> requests = new ArrayList<>();
            while (results.next()) {
                requests.add(read(results));
            }
            return List.copyOf(requests);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list service requests", failure);
        }
    }

    @Override
    public void save(UUID id, ServiceRequest request) {
        if (!Objects.requireNonNull(id, "id").equals(request.requestId())) {
            throw new IllegalArgumentException("Repository key must match requestId");
        }
        String sql = "UPDATE service_requests SET student_id = ?, category = ?, priority = ?, "
                + "status = ?, assigned_to = ?, created_at = ?, updated_at = ? WHERE request_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            bindUpdate(update, request, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO service_requests (student_id, category, priority, status, "
                                + "assigned_to, created_at, updated_at, request_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindUpdate(insert, request, id);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save service request", failure);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM service_requests WHERE request_id = ?")) {
            statement.setString(1, id.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete service request", failure);
        }
    }

    private static void bindUpdate(PreparedStatement statement, ServiceRequest request, UUID id)
            throws SQLException {
        statement.setString(1, request.studentId());
        statement.setString(2, request.category());
        statement.setString(3, request.priority().name());
        statement.setString(4, request.status().name());
        statement.setString(5, request.assignedTo());
        statement.setTimestamp(6, JdbcSupport.timestamp(request.createdAt()));
        statement.setTimestamp(7, JdbcSupport.timestamp(request.updatedAt()));
        statement.setString(8, id.toString());
    }

    private static ServiceRequest read(ResultSet results) throws SQLException {
        return new ServiceRequest(UUID.fromString(results.getString("request_id")),
                results.getString("student_id"), results.getString("category"),
                RequestPriority.valueOf(results.getString("priority")),
                RequestStatus.valueOf(results.getString("status")),
                results.getString("assigned_to"),
                JdbcSupport.instant(results.getTimestamp("created_at")),
                JdbcSupport.instant(results.getTimestamp("updated_at")));
    }
}
