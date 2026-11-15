package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.RegistrationStatus;
import edu.nipa.scasp.domain.WaitlistEntry;

public final class JdbcRegistrationRepository
        implements RegistrationRepository {

    public JdbcRegistrationRepository() {
    }

    @Override
    public RegistrationAttempt register(
            Connection connection,
            String studentId,
            String courseCode,
            Instant now
    ) throws SQLException {

        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(now, "now");
        studentId = requireText(studentId, "studentId");
        courseCode = requireText(courseCode, "courseCode");

        // Serialize registration operations for this course.
        CourseCapacity course = lockCourse(connection, courseCode);

        if (course.status() != CourseStatus.OPEN) {
            throw new IllegalStateException(
                    "Course is not open: " + courseCode
            );
        }

        Registration existing = findRegistration(
                connection, studentId, courseCode
        );

        if (existing != null
                && existing.status() == RegistrationStatus.ENROLLED) {
            throw new IllegalStateException(
                    "Student is already registered for this course"
            );
        }

        if (isWaitlisted(connection, studentId, courseCode)) {
            throw new IllegalStateException(
                    "Student is already on the course waitlist"
            );
        }

        int enrolled = countEnrolled(connection, courseCode);
        int waiting = countWaitlisted(connection, courseCode);

        // Do not bypass students already waiting in the queue.
        if (enrolled < course.capacity() && waiting == 0) {
            Registration registration;

            if (existing != null) {
                registration = existing.transitionTo(
                        RegistrationStatus.ENROLLED
                );
                updateRegistrationStatus(
                        connection,
                        registration.registrationId(),
                        RegistrationStatus.ENROLLED
                );
            } else {
                registration = new Registration(
                        UUID.randomUUID(),
                        studentId,
                        courseCode,
                        RegistrationStatus.ENROLLED,
                        now
                );
                insertRegistration(connection, registration);
            }

            return new RegistrationAttempt(registration, null);
        }

        int position = nextWaitlistPosition(connection, courseCode);

        WaitlistEntry entry = new WaitlistEntry(
                UUID.randomUUID(),
                studentId,
                courseCode,
                position,
                now
        );

        insertWaitlistEntry(connection, entry);

        return new RegistrationAttempt(null, entry);
    }

    @Override
    public Registration withdraw(
            Connection connection,
            String studentId,
            String courseCode,
            Instant now
    ) throws SQLException {

        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(now, "now");
        studentId = requireText(studentId, "studentId");
        courseCode = requireText(courseCode, "courseCode");

        // Use the same course lock as registration.
        CourseCapacity course = lockCourse(connection, courseCode);

        Registration current = findRegistration(
                connection, studentId, courseCode
        );

        if (current == null
                || current.status() != RegistrationStatus.ENROLLED) {
            throw new IllegalStateException(
                    "No active registration to withdraw"
            );
        }

        Registration withdrawn = current.transitionTo(
                RegistrationStatus.WITHDRAWN
        );

        updateRegistrationStatus(
                connection,
                withdrawn.registrationId(),
                RegistrationStatus.WITHDRAWN
        );

        if (course.status() == CourseStatus.OPEN
                && countEnrolled(connection, courseCode) < course.capacity()) {
            promoteNext(connection, courseCode, now);
        }

        return withdrawn;
    }

    @Override
    public List<Registration> findByStudentId(
            Connection connection,
            String studentId
    ) throws SQLException {

        Objects.requireNonNull(connection, "connection");
        studentId = requireText(studentId, "studentId");

        String sql = """
                SELECT registration_id, student_id, course_code,
                       status, created_at
                FROM registrations
                WHERE student_id = ?
                ORDER BY created_at DESC
                """;

        List<Registration> registrations = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    registrations.add(mapRegistration(result));
                }
            }
        }

        return List.copyOf(registrations);
    }

    @Override
    public List<WaitlistEntry> findWaitlistByStudentId(
            Connection connection,
            String studentId
    ) throws SQLException {

        Objects.requireNonNull(connection, "connection");
        studentId = requireText(studentId, "studentId");

        String sql = """
                SELECT entry_id, student_id, course_code,
                       position, created_at
                FROM waitlist_entries
                WHERE student_id = ?
                ORDER BY course_code, position
                """;

        List<WaitlistEntry> entries = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(mapWaitlistEntry(result));
                }
            }
        }

        return List.copyOf(entries);
    }

    private CourseCapacity lockCourse(
            Connection connection,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT capacity, status
                FROM courses
                WHERE course_code = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Unknown course: " + courseCode
                    );
                }

                return new CourseCapacity(
                        result.getInt("capacity"),
                        CourseStatus.valueOf(result.getString("status"))
                );
            }
        }
    }

    private Registration findRegistration(
            Connection connection,
            String studentId,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT registration_id, student_id, course_code,
                       status, created_at
                FROM registrations
                WHERE student_id = ? AND course_code = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRegistration(result) : null;
            }
        }
    }

    private boolean isWaitlisted(
            Connection connection,
            String studentId,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM waitlist_entries
                WHERE student_id = ? AND course_code = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private int countEnrolled(
            Connection connection,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM registrations
                WHERE course_code = ? AND status = 'ENROLLED'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int countWaitlisted(
            Connection connection,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM waitlist_entries
                WHERE course_code = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int nextWaitlistPosition(
            Connection connection,
            String courseCode
    ) throws SQLException {

        String sql = """
                SELECT COALESCE(MAX(position), 0) + 1
                FROM waitlist_entries
                WHERE course_code = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void insertRegistration(
            Connection connection,
            Registration registration
    ) throws SQLException {

        String sql = """
                INSERT INTO registrations
                    (registration_id, student_id, course_code,
                     status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, registration.registrationId().toString());
            statement.setString(2, registration.studentId());
            statement.setString(3, registration.courseCode());
            statement.setString(4, registration.status().name());
            statement.setTimestamp(
                    5, Timestamp.from(registration.createdAt())
            );

            statement.executeUpdate();
        }
    }

    private void updateRegistrationStatus(
            Connection connection,
            UUID registrationId,
            RegistrationStatus status
    ) throws SQLException {

        String sql = """
                UPDATE registrations
                SET status = ?
                WHERE registration_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setString(2, registrationId.toString());

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Registration status update affected no row"
                );
            }
        }
    }

    private void insertWaitlistEntry(
            Connection connection,
            WaitlistEntry entry
    ) throws SQLException {

        String sql = """
                INSERT INTO waitlist_entries
                    (entry_id, student_id, course_code,
                     position, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, entry.entryId().toString());
            statement.setString(2, entry.studentId());
            statement.setString(3, entry.courseCode());
            statement.setInt(4, entry.position());
            statement.setTimestamp(
                    5, Timestamp.from(entry.createdAt())
            );

            statement.executeUpdate();
        }
    }

    private void promoteNext(
            Connection connection,
            String courseCode,
            Instant now
    ) throws SQLException {

        String sql = """
                SELECT entry_id, student_id, course_code,
                       position, created_at
                FROM waitlist_entries
                WHERE course_code = ?
                ORDER BY position, created_at
                LIMIT 1
                FOR UPDATE
                """;

        WaitlistEntry next;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return;
                }

                next = mapWaitlistEntry(result);
            }
        }

        // Remove the queue entry before promoting the student.
        String deleteSql = """
                DELETE FROM waitlist_entries
                WHERE entry_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(deleteSql)) {

            statement.setString(1, next.entryId().toString());
            statement.executeUpdate();
        }

        Registration previous = findRegistration(
                connection, next.studentId(), courseCode
        );

        if (previous != null) {
            updateRegistrationStatus(
                    connection,
                    previous.registrationId(),
                    RegistrationStatus.ENROLLED
            );
        } else {
            Registration promoted = new Registration(
                    UUID.randomUUID(),
                    next.studentId(),
                    courseCode,
                    RegistrationStatus.ENROLLED,
                    now
            );

            insertRegistration(connection, promoted);
        }

        // Close the gap left by the removed queue entry.
        String reindexSql = """
                UPDATE waitlist_entries
                SET position = position - 1
                WHERE course_code = ? AND position > ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(reindexSql)) {

            statement.setString(1, courseCode);
            statement.setInt(2, next.position());
            statement.executeUpdate();
        }
    }

    private Registration mapRegistration(
            ResultSet result
    ) throws SQLException {

        return new Registration(
                UUID.fromString(result.getString("registration_id")),
                result.getString("student_id"),
                result.getString("course_code"),
                RegistrationStatus.valueOf(result.getString("status")),
                result.getTimestamp("created_at").toInstant()
        );
    }

    private WaitlistEntry mapWaitlistEntry(
            ResultSet result
    ) throws SQLException {

        return new WaitlistEntry(
                UUID.fromString(result.getString("entry_id")),
                result.getString("student_id"),
                result.getString("course_code"),
                result.getInt("position"),
                result.getTimestamp("created_at").toInstant()
        );
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value;
    }

    private record CourseCapacity(int capacity, CourseStatus status) {
    }
}