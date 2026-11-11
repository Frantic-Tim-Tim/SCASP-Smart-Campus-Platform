package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationKey;
import edu.nipa.scasp.domain.RegistrationRepository;
import edu.nipa.scasp.domain.RegistrationStatus;
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

public final class JdbcRegistrationRepository implements RegistrationRepository {
    private final DataSource dataSource;

    public JdbcRegistrationRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Registration> findById(RegistrationKey key) {
        String sql = "SELECT registration_id, student_id, course_code, status, created_at "
                + "FROM registrations WHERE student_id = ? AND course_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKey(statement, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find registration", failure);
        }
    }

    @Override
    public List<Registration> findAll() {
        String sql = "SELECT registration_id, student_id, course_code, status, created_at "
                + "FROM registrations ORDER BY course_code, student_id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Registration> registrations = new ArrayList<>();
            while (results.next()) {
                registrations.add(read(results));
            }
            return List.copyOf(registrations);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list registrations", failure);
        }
    }

    @Override
    public void save(RegistrationKey key, Registration registration) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(registration, "registration");
        if (!key.studentId().equals(registration.studentId())
                || !key.courseCode().equals(registration.courseCode())) {
            throw new IllegalArgumentException("Repository key must match registration identity");
        }
        String sql = "UPDATE registrations SET registration_id = ?, status = ?, created_at = ? "
                + "WHERE student_id = ? AND course_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            update.setString(1, registration.registrationId().toString());
            update.setString(2, registration.status().name());
            update.setTimestamp(3, JdbcSupport.timestamp(registration.createdAt()));
            bindKey(update, key, 4);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO registrations (registration_id, status, created_at, "
                                + "student_id, course_code) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setString(1, registration.registrationId().toString());
                    insert.setString(2, registration.status().name());
                    insert.setTimestamp(3, JdbcSupport.timestamp(registration.createdAt()));
                    bindKey(insert, key, 4);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save registration", failure);
        }
    }

    @Override
    public boolean deleteById(RegistrationKey key) {
        String sql = "DELETE FROM registrations WHERE student_id = ? AND course_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKey(statement, key);
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete registration", failure);
        }
    }

    private static void bindKey(PreparedStatement statement, RegistrationKey key)
            throws SQLException {
        bindKey(statement, key, 1);
    }

    private static void bindKey(PreparedStatement statement, RegistrationKey key, int start)
            throws SQLException {
        statement.setString(start, key.studentId());
        statement.setString(start + 1, key.courseCode());
    }

    private static Registration read(ResultSet result) throws SQLException {
        return new Registration(UUID.fromString(result.getString("registration_id")),
                result.getString("student_id"), result.getString("course_code"),
                RegistrationStatus.valueOf(result.getString("status")),
                JdbcSupport.instant(result.getTimestamp("created_at")));
    }
}
