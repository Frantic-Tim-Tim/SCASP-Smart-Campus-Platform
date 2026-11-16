package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JdbcServiceRequestRepository
        implements ServiceRequestRepository {

    private final ConnectionFactory connectionFactory;

    public JdbcServiceRequestRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory"
        );
    }

    @Override
    public void create(ServiceRequest request) throws SQLException {
        String sql = """
                INSERT INTO service_requests (
                    request_id, student_id, title, description,
                    category, priority, status, assigned_to,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, request.requestId().toString());
            statement.setString(2, request.studentId());
            statement.setString(3, request.title());
            statement.setString(4, request.description());
            statement.setString(5, request.category());
            statement.setString(6, request.priority().name());
            statement.setString(7, request.status().name());
            statement.setString(8, request.assignedTo());
            statement.setTimestamp(9, Timestamp.from(request.createdAt()));
            statement.setTimestamp(10, Timestamp.from(request.updatedAt()));

            statement.executeUpdate();
        }
    }

    @Override
    public Optional<ServiceRequest> findById(UUID requestId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM service_requests
                WHERE request_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, requestId.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapRequest(result));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<ServiceRequest> findByStudentId(String studentId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM service_requests
                WHERE student_id = ?
                ORDER BY created_at DESC
                """;

        List<ServiceRequest> requests = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    requests.add(mapRequest(result));
                }
            }
        }

        return requests;
    }

    @Override
    public List<ServiceRequest> findAll() throws SQLException {
        String sql = """
                SELECT *
                FROM service_requests
                ORDER BY created_at DESC
                """;

        List<ServiceRequest> requests = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                requests.add(mapRequest(result));
            }
        }

        return requests;
    }

    @Override
    public boolean update(ServiceRequest request) throws SQLException {
        String sql = """
                UPDATE service_requests
                SET title = ?,
                    description = ?,
                    category = ?,
                    priority = ?,
                    status = ?,
                    assigned_to = ?,
                    updated_at = ?
                WHERE request_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, request.title());
            statement.setString(2, request.description());
            statement.setString(3, request.category());
            statement.setString(4, request.priority().name());
            statement.setString(5, request.status().name());
            statement.setString(6, request.assignedTo());
            statement.setTimestamp(7, Timestamp.from(request.updatedAt()));
            statement.setString(8, request.requestId().toString());

            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public void addUpdate(ServiceRequestUpdate update) throws SQLException {
        String sql = """
                INSERT INTO service_request_updates (
                    update_id, request_id, author_user_id, note, created_at
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, update.updateId().toString());
            statement.setString(2, update.requestId().toString());
            statement.setString(3, update.authorUserId());
            statement.setString(4, update.note());
            statement.setTimestamp(5, Timestamp.from(update.createdAt()));

            statement.executeUpdate();
        }
    }

    @Override
    public List<ServiceRequestUpdate> findUpdates(UUID requestId)
            throws SQLException {

        String sql = """
                SELECT update_id, request_id, author_user_id, note, created_at
                FROM service_request_updates
                WHERE request_id = ?
                ORDER BY created_at ASC
                """;

        List<ServiceRequestUpdate> updates = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, requestId.toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    updates.add(mapUpdate(result));
                }
            }
        }

        return updates;
    }

    private ServiceRequest mapRequest(ResultSet result) throws SQLException {
        return new ServiceRequest(
                UUID.fromString(result.getString("request_id")),
                result.getString("student_id"),
                result.getString("title"),
                result.getString("description"),
                result.getString("category"),
                RequestPriority.valueOf(result.getString("priority")),
                RequestStatus.valueOf(result.getString("status")),
                result.getString("assigned_to"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    private ServiceRequestUpdate mapUpdate(ResultSet result)
            throws SQLException {

        return new ServiceRequestUpdate(
                UUID.fromString(result.getString("update_id")),
                UUID.fromString(result.getString("request_id")),
                result.getString("author_user_id"),
                result.getString("note"),
                result.getTimestamp("created_at").toInstant()
        );
    }
}
