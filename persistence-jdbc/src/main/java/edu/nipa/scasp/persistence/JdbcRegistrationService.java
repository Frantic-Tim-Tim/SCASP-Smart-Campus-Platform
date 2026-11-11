package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.RegistrationStatus;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.domain.WaitlistEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcRegistrationService {
    private final DataSource dataSource;

    public JdbcRegistrationService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public RegistrationAttempt register(User actor, String studentId, String courseCode,
                                        Instant at) {
        Objects.requireNonNull(at, "at");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Course course = lockCourse(connection, courseCode);
                authorize(connection, actor, studentId);
                if (!course.open()) {
                    throw new IllegalStateException("Course is not open: " + courseCode);
                }
                ExistingRegistration existing = existingRegistration(connection, studentId,
                        courseCode);
                if (existing != null && existing.status() == RegistrationStatus.ENROLLED) {
                    throw new IllegalStateException("Student is already registered");
                }
                if (isWaitlisted(connection, studentId, courseCode)) {
                    throw new IllegalStateException("Student is already waitlisted");
                }
                int waiting = countWaitlist(connection, courseCode);
                RegistrationAttempt result;
                if (countEnrolled(connection, courseCode) < course.capacity() && waiting == 0) {
                    result = new RegistrationAttempt(enrol(connection, existing, studentId,
                            courseCode, at), null);
                } else {
                    WaitlistEntry entry = new WaitlistEntry(UUID.randomUUID(), studentId,
                            courseCode, nextPosition(connection, courseCode), at);
                    insertWaitlist(connection, entry);
                    result = new RegistrationAttempt(null, entry);
                }
                insertAudit(connection, actor.userId(), result.isWaitlisted()
                        ? "WAITLISTED" : "REGISTERED", result.isWaitlisted()
                        ? "WaitlistEntry" : "Registration", studentId + ":" + courseCode, at);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("register student transaction", failure);
        }
    }

    public Registration withdraw(User actor, String studentId, String courseCode, Instant at) {
        Objects.requireNonNull(at, "at");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Course course = lockCourse(connection, courseCode);
                authorize(connection, actor, studentId);
                ExistingRegistration current = existingRegistration(connection, studentId,
                        courseCode);
                if (current == null || current.status() != RegistrationStatus.ENROLLED) {
                    throw new IllegalStateException("No active registration to withdraw");
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE registrations SET status = 'WITHDRAWN' "
                                + "WHERE student_id = ? AND course_code = ?")) {
                    update.setString(1, studentId);
                    update.setString(2, courseCode);
                    update.executeUpdate();
                }
                WaitlistEntry next = firstWaitlisted(connection, courseCode);
                if (next != null && course.open()) {
                    promote(connection, next, at);
                }
                insertAudit(connection, actor.userId(), "WITHDRAWN", "Registration",
                        studentId + ":" + courseCode, at);
                Registration withdrawn = new Registration(current.id(), studentId, courseCode,
                        RegistrationStatus.WITHDRAWN, current.createdAt());
                connection.commit();
                return withdrawn;
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("withdraw student transaction", failure);
        }
    }

    private static Course lockCourse(Connection connection, String courseCode) throws SQLException {
        String sql = "SELECT capacity, status FROM courses WHERE course_code = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Unknown course: " + courseCode);
                }
                return new Course(result.getInt("capacity"),
                        "OPEN".equals(result.getString("status")));
            }
        }
    }

    private static void authorize(Connection connection, User actor, String studentId)
            throws SQLException {
        Objects.requireNonNull(actor, "actor");
        if (actor.status() != UserStatus.ACTIVE || actor.role() != UserRole.STUDENT) {
            throw new SecurityException("Only an active student may manage registrations");
        }
        String sql = "SELECT u.status, u.role, s.user_id FROM users u "
                + "JOIN students s ON s.user_id = u.user_id "
                + "WHERE u.user_id = ? AND s.student_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actor.userId());
            statement.setString(2, studentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"ACTIVE".equals(result.getString("status"))
                        || !"STUDENT".equals(result.getString("role"))
                        || !actor.userId().equals(result.getString("user_id"))) {
                    throw new SecurityException("Student may only manage their own registration");
                }
            }
        }
    }

    private static ExistingRegistration existingRegistration(Connection connection,
                                                              String studentId, String courseCode)
            throws SQLException {
        String sql = "SELECT registration_id, status, created_at FROM registrations "
                + "WHERE student_id = ? AND course_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new ExistingRegistration(UUID.fromString(result.getString("registration_id")),
                        RegistrationStatus.valueOf(result.getString("status")),
                        JdbcSupport.instant(result.getTimestamp("created_at")));
            }
        }
    }

    private static boolean isWaitlisted(Connection connection, String studentId, String courseCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM waitlist_entries WHERE student_id = ? AND course_code = ?")) {
            statement.setString(1, studentId);
            statement.setString(2, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static int countEnrolled(Connection connection, String courseCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM registrations WHERE course_code = ? AND status = 'ENROLLED'")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int countWaitlist(Connection connection, String courseCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM waitlist_entries WHERE course_code = ?")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int nextPosition(Connection connection, String courseCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(position), 0) + 1 FROM waitlist_entries WHERE course_code = ?")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static Registration enrol(Connection connection, ExistingRegistration existing,
                                      String studentId, String courseCode, Instant at)
            throws SQLException {
        UUID id = existing == null ? UUID.randomUUID() : existing.id();
        Instant createdAt = existing == null ? at : existing.createdAt();
        if (existing == null) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO registrations (registration_id, student_id, course_code, "
                            + "status, created_at) VALUES (?, ?, ?, 'ENROLLED', ?)")) {
                insert.setString(1, id.toString());
                insert.setString(2, studentId);
                insert.setString(3, courseCode);
                insert.setTimestamp(4, JdbcSupport.timestamp(createdAt));
                insert.executeUpdate();
            }
        } else {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE registrations SET status = 'ENROLLED' "
                            + "WHERE student_id = ? AND course_code = ?")) {
                update.setString(1, studentId);
                update.setString(2, courseCode);
                update.executeUpdate();
            }
        }
        return new Registration(id, studentId, courseCode, RegistrationStatus.ENROLLED, createdAt);
    }

    private static void insertWaitlist(Connection connection, WaitlistEntry entry)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO waitlist_entries (entry_id, student_id, course_code, position, "
                        + "created_at) VALUES (?, ?, ?, ?, ?)")) {
            insert.setString(1, entry.entryId().toString());
            insert.setString(2, entry.studentId());
            insert.setString(3, entry.courseCode());
            insert.setInt(4, entry.position());
            insert.setTimestamp(5, JdbcSupport.timestamp(entry.createdAt()));
            insert.executeUpdate();
        }
    }

    private static WaitlistEntry firstWaitlisted(Connection connection, String courseCode)
            throws SQLException {
        String sql = "SELECT entry_id, student_id, course_code, position, created_at "
                + "FROM waitlist_entries WHERE course_code = ? ORDER BY position";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new WaitlistEntry(UUID.fromString(result.getString("entry_id")),
                        result.getString("student_id"), result.getString("course_code"),
                        result.getInt("position"),
                        JdbcSupport.instant(result.getTimestamp("created_at")));
            }
        }
    }

    private static void promote(Connection connection, WaitlistEntry next, Instant at)
            throws SQLException {
        ExistingRegistration previous = existingRegistration(connection, next.studentId(),
                next.courseCode());
        enrol(connection, previous, next.studentId(), next.courseCode(), at);
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM waitlist_entries WHERE entry_id = ?")) {
            delete.setString(1, next.entryId().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement shift = connection.prepareStatement(
                "UPDATE waitlist_entries SET position = position - 1 "
                        + "WHERE course_code = ? AND position > ?")) {
            shift.setString(1, next.courseCode());
            shift.setInt(2, next.position());
            shift.executeUpdate();
        }
    }

    private static void insertAudit(Connection connection, String actor, String action,
                                    String entityType, String entityId, Instant at)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO audit_events (event_id, actor, action, entity_type, entity_id, "
                        + "occurred_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, UUID.randomUUID().toString());
            insert.setString(2, actor);
            insert.setString(3, action);
            insert.setString(4, entityType);
            insert.setString(5, entityId);
            insert.setTimestamp(6, JdbcSupport.timestamp(at));
            insert.executeUpdate();
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private record Course(int capacity, boolean open) {
    }

    private record ExistingRegistration(UUID id, RegistrationStatus status, Instant createdAt) {
    }
}
