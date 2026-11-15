package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.RegistrationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRegistrationRepositoryTest {

    private ConnectionFactory factory;
    private JdbcRegistrationRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "registration_test_"
                + UUID.randomUUID().toString().replace("-", "");

        DatabaseConfig config = new DatabaseConfig(
                "jdbc:h2:mem:" + databaseName
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;"
                        + "DATABASE_TO_UPPER=TRUE",
                "sa",
                ""
        );

        factory = new ConnectionFactory(config);

        new DatabaseInitializer(factory).initialize();

        repository = new JdbcRegistrationRepository();
    }

    @Test
    void registersStudentWhenCourseHasCapacity() throws Exception {
        seedStudent("STU001", "USER001");
        seedCourse("CS101", 2, "OPEN");

        Instant now = Instant.parse("2026-11-14T08:00:00.123456Z");

        RegistrationAttempt attempt = inTransaction(connection ->
                repository.register(connection, "STU001", "CS101", now)
        );

        assertNotNull(attempt.registration());
        assertNull(attempt.waitlistEntry());
        assertEquals(
                RegistrationStatus.ENROLLED,
                attempt.registration().status()
        );
        assertEquals(now, attempt.registration().createdAt());

        var registrations = inTransaction(connection ->
                repository.findByStudentId(connection, "STU001")
        );

        assertEquals(1, registrations.size());
    }

    @Test
    void placesStudentOnWaitlistWhenCourseIsFull() throws Exception {
        seedStudent("STU001", "USER001");
        seedStudent("STU002", "USER002");
        seedCourse("CS101", 1, "OPEN");

        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        // First student takes the only available place.
        inTransaction(connection ->
                repository.register(connection, "STU001", "CS101", now)
        );

        // Second student must join the waitlist.
        RegistrationAttempt attempt = inTransaction(connection ->
                repository.register(
                        connection,
                        "STU002",
                        "CS101",
                        now.plusSeconds(60)
                )
        );

        assertNull(attempt.registration());
        assertNotNull(attempt.waitlistEntry());
        assertEquals(1, attempt.waitlistEntry().position());
        assertEquals("STU002", attempt.waitlistEntry().studentId());

        var waitlist = inTransaction(connection ->
                repository.findWaitlistByStudentId(connection, "STU002")
        );

        assertEquals(1, waitlist.size());
    }

    @Test
    void rejectsDuplicateActiveRegistration() throws Exception {
        seedStudent("STU001", "USER001");
        seedCourse("CS101", 2, "OPEN");

        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        inTransaction(connection ->
                repository.register(connection, "STU001", "CS101", now)
        );

        assertThrows(IllegalStateException.class, () ->
                inTransaction(connection ->
                        repository.register(
                                connection,
                                "STU001",
                                "CS101",
                                now.plusSeconds(60)
                        )
                )
        );
    }

    @Test
    void withdrawalPromotesNextWaitlistedStudent() throws Exception {
        seedStudent("STU001", "USER001");
        seedStudent("STU002", "USER002");
        seedCourse("CS101", 1, "OPEN");

        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        // Student 1 is enrolled.
        inTransaction(connection ->
                repository.register(connection, "STU001", "CS101", now)
        );

        // Student 2 joins the waitlist.
        inTransaction(connection ->
                repository.register(
                        connection,
                        "STU002",
                        "CS101",
                        now.plusSeconds(60)
                )
        );

        // Student 1 withdraws.
        Registration withdrawn = inTransaction(connection ->
                repository.withdraw(
                        connection,
                        "STU001",
                        "CS101",
                        now.plusSeconds(120)
                )
        );

        assertEquals(RegistrationStatus.WITHDRAWN, withdrawn.status());

        // Student 2 should automatically be promoted.
        var studentTwoRegistrations = inTransaction(connection ->
                repository.findByStudentId(connection, "STU002")
        );

        assertEquals(1, studentTwoRegistrations.size());
        assertEquals(
                RegistrationStatus.ENROLLED,
                studentTwoRegistrations.get(0).status()
        );

        var studentTwoWaitlist = inTransaction(connection ->
                repository.findWaitlistByStudentId(connection, "STU002")
        );

        assertTrue(studentTwoWaitlist.isEmpty());
    }

    @Test
    void reactivatesWithdrawnRegistrationWithSameIdAndCreationTime()
            throws Exception {

        seedStudent("STU001", "USER001");
        seedCourse("CS101", 2, "OPEN");

        Instant originalTime =
                Instant.parse("2026-11-14T08:00:00.123456Z");

        Instant withdrawalTime =
                Instant.parse("2026-11-14T09:00:00.123456Z");

        Instant reactivationTime =
                Instant.parse("2026-11-14T10:00:00.123456Z");

        RegistrationAttempt firstAttempt = inTransaction(connection ->
                repository.register(
                        connection, "STU001", "CS101", originalTime
                )
        );

        UUID originalId = firstAttempt.registration().registrationId();

        inTransaction(connection ->
                repository.withdraw(
                        connection, "STU001", "CS101", withdrawalTime
                )
        );

        RegistrationAttempt secondAttempt = inTransaction(connection ->
                repository.register(
                        connection, "STU001", "CS101", reactivationTime
                )
        );

        Registration reactivated = secondAttempt.registration();

        assertNotNull(reactivated);
        assertEquals(originalId, reactivated.registrationId());
        assertEquals(originalTime, reactivated.createdAt());
        assertEquals(
                RegistrationStatus.ENROLLED,
                reactivated.status()
        );
    }

    @Test
    void rejectsRegistrationForClosedCourse() throws Exception {
        seedStudent("STU001", "USER001");
        seedCourse("CS101", 2, "CLOSED");

        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        assertThrows(IllegalStateException.class, () ->
                inTransaction(connection ->
                        repository.register(
                                connection, "STU001", "CS101", now
                        )
                )
        );
    }

    @Test
    void rejectsRegistrationForUnknownCourse() throws Exception {
        seedStudent("STU001", "USER001");

        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                inTransaction(connection ->
                        repository.register(
                                connection, "STU001", "UNKNOWN", now
                        )
                )
        );
    }

    private void seedStudent(String studentId, String userId)
            throws SQLException {

        try (Connection connection = factory.getConnection()) {

            String userSql = """
                    INSERT INTO users (user_id, name, role, status)
                    VALUES (?, ?, 'STUDENT', 'ACTIVE')
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(userSql)) {

                statement.setString(1, userId);
                statement.setString(2, "Test Student " + studentId);
                statement.executeUpdate();
            }

            String studentSql = """
                    INSERT INTO students
                        (student_id, user_id, programme, year_of_study)
                    VALUES (?, ?, 'Computer Science', 3)
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(studentSql)) {

                statement.setString(1, studentId);
                statement.setString(2, userId);
                statement.executeUpdate();
            }
        }
    }

    private void seedCourse(
            String courseCode,
            int capacity,
            String status
    ) throws SQLException {

        String sql = """
                INSERT INTO courses
                    (course_code, title, capacity, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = factory.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);
            statement.setString(2, "Test Course");
            statement.setInt(3, capacity);
            statement.setString(4, status);

            statement.executeUpdate();
        }
    }

    private <T> T inTransaction(SqlWork<T> work) throws Exception {

        try (Connection connection = factory.getConnection()) {

            connection.setAutoCommit(false);

            try {
                T result = work.execute(connection);
                connection.commit();
                return result;

            } catch (Exception | Error exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
