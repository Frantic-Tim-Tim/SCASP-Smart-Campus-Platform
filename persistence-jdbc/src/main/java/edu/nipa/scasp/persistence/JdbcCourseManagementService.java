package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcCourseManagementService {
    private final DataSource dataSource;

    public JdbcCourseManagementService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public void createOrUpdate(User actor, Course course) {
        requireStaff(actor);
        Objects.requireNonNull(course, "course");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                authorize(connection, actor);
                boolean exists = lockCourse(connection, course.courseCode());
                if (exists) {
                    int enrolled = enrolledCount(connection, course.courseCode());
                    if (course.capacity() < enrolled) {
                        throw new IllegalArgumentException(
                                "Capacity cannot be lower than current enrolment");
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE courses SET title = ?, capacity = ?, status = ? "
                                    + "WHERE course_code = ?")) {
                        update.setString(1, course.title());
                        update.setInt(2, course.capacity());
                        update.setString(3, course.status().name());
                        update.setString(4, course.courseCode());
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO courses (course_code, title, capacity, status) "
                                    + "VALUES (?, ?, ?, ?)")) {
                        insert.setString(1, course.courseCode());
                        insert.setString(2, course.title());
                        insert.setInt(3, course.capacity());
                        insert.setString(4, course.status().name());
                        insert.executeUpdate();
                    }
                }
                writeAudit(connection, actor.userId(), exists ? "COURSE_UPDATED" : "COURSE_CREATED",
                        course.courseCode(), Instant.now());
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("manage course transaction", failure);
        }
    }

    private static boolean lockCourse(Connection connection, String courseCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT course_code FROM courses WHERE course_code = ? FOR UPDATE")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void authorize(Connection connection, User actor) throws SQLException {
        String sql = "SELECT role, status FROM users WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actor.userId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"ACTIVE".equals(result.getString("status"))
                        || "STUDENT".equals(result.getString("role"))
                        || !actor.role().name().equals(result.getString("role"))) {
                    throw new SecurityException("An active staff user is required");
                }
            }
        }
    }

    private static int enrolledCount(Connection connection, String courseCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM registrations WHERE course_code = ? AND status = 'ENROLLED'")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void writeAudit(Connection connection, String actor, String action,
                                   String courseCode, Instant at) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit_events (event_id, actor, action, entity_type, entity_id, "
                        + "occurred_at) VALUES (?, ?, ?, 'Course', ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, actor);
            statement.setString(3, action);
            statement.setString(4, courseCode);
            statement.setTimestamp(5, JdbcSupport.timestamp(at));
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static void requireStaff(User actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor.status() != UserStatus.ACTIVE || actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
    }
}
