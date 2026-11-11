package edu.nipa.scasp.enterprise;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;

@Stateless
@DeclareRoles("STUDENT")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RegistrationBean implements RegistrationLocal {
    private static final Logger LOGGER = Logger.getLogger(RegistrationBean.class.getName());

    @Resource(lookup = "java:jboss/datasources/ScaspDS")
    private DataSource dataSource;

    @Resource
    private SessionContext sessionContext;

    @PostConstruct
    void initialized() {
        LOGGER.info("SCASP registration bean initialized");
    }

    @PreDestroy
    void shuttingDown() {
        LOGGER.info("SCASP registration bean is shutting down");
    }

    @Override
    @RolesAllowed("STUDENT")
    public RegistrationResult register(String studentId, String courseCode) {
        String actorId = sessionContext.getCallerPrincipal().getName();
        try (Connection connection = dataSource.getConnection()) {
            authorizeStudent(connection, actorId, studentId);
            int capacity = lockOpenCourse(connection, courseCode);
            ensureNotAlreadyRegistered(connection, studentId, courseCode);
            int enrolled = count(connection, "SELECT COUNT(*) FROM registrations "
                    + "WHERE course_code = ? AND status = 'ENROLLED'", courseCode);
            int waiting = count(connection, "SELECT COUNT(*) FROM waitlist_entries "
                    + "WHERE course_code = ?", courseCode);
            Instant now = Instant.now();
            RegistrationResult result;
            if (enrolled < capacity && waiting == 0) {
                String id = UUID.randomUUID().toString();
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO registrations (registration_id, student_id, course_code, "
                                + "status, created_at) VALUES (?, ?, ?, 'ENROLLED', ?)")) {
                    insert.setString(1, id);
                    insert.setString(2, studentId);
                    insert.setString(3, courseCode);
                    insert.setTimestamp(4, java.sql.Timestamp.from(now));
                    insert.executeUpdate();
                }
                result = new RegistrationResult("ENROLLED", id, 0);
            } else {
                int position = nextPosition(connection, courseCode);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO waitlist_entries (entry_id, student_id, course_code, "
                                + "position, created_at) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setString(1, UUID.randomUUID().toString());
                    insert.setString(2, studentId);
                    insert.setString(3, courseCode);
                    insert.setInt(4, position);
                    insert.setTimestamp(5, java.sql.Timestamp.from(now));
                    insert.executeUpdate();
                }
                result = new RegistrationResult("WAITLISTED", null, position);
            }
            writeAudit(connection, actorId, result.outcome(), studentId + ":" + courseCode, now);
            return result;
        } catch (SQLException failure) {
            throw new EJBException("Registration transaction failed", failure);
        }
    }

    private static void authorizeStudent(Connection connection, String actorId, String studentId)
            throws SQLException {
        String sql = "SELECT u.status, u.role FROM users u JOIN students s ON s.user_id = u.user_id "
                + "WHERE u.user_id = ? AND s.student_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actorId);
            statement.setString(2, studentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"ACTIVE".equals(result.getString("status"))
                        || !"STUDENT".equals(result.getString("role"))) {
                    throw new EJBException("Only an active student may register themselves");
                }
            }
        }
    }

    private static int lockOpenCourse(Connection connection, String courseCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT capacity, status FROM courses WHERE course_code = ? FOR UPDATE")) {
            statement.setString(1, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new EJBException("Unknown course: " + courseCode);
                }
                if (!"OPEN".equals(result.getString("status"))) {
                    throw new EJBException("Course is not open: " + courseCode);
                }
                return result.getInt("capacity");
            }
        }
    }

    private static void ensureNotAlreadyRegistered(Connection connection, String studentId,
                                                   String courseCode) throws SQLException {
        String sql = "SELECT status FROM registrations WHERE student_id = ? AND course_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && "ENROLLED".equals(result.getString("status"))) {
                    throw new EJBException("Student is already registered");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM waitlist_entries WHERE student_id = ? AND course_code = ?")) {
            statement.setString(1, studentId);
            statement.setString(2, courseCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new EJBException("Student is already waitlisted");
                }
            }
        }
    }

    private static int count(Connection connection, String sql, String courseCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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

    private static void writeAudit(Connection connection, String actorId, String action,
                                   String entityId, Instant now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit_events (event_id, actor, action, entity_type, entity_id, "
                        + "occurred_at) VALUES (?, ?, ?, 'Registration', ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, actorId);
            statement.setString(3, action);
            statement.setString(4, entityId);
            statement.setTimestamp(5, java.sql.Timestamp.from(now));
            statement.executeUpdate();
        }
    }

}
