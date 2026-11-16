package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcAcademicReportsRepository {

    public List<CourseEnrollmentReport> findCourseEnrollmentSummary(
            Connection connection
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection");

        String sql = """
                SELECT
                    c.course_code,
                    c.title,
                    c.capacity,
                    c.status,
                    COALESCE(r.enrolled_count, 0) AS enrolled_count,
                    COALESCE(w.waitlist_count, 0) AS waitlist_count
                FROM courses c
                LEFT JOIN (
                    SELECT course_code, COUNT(*) AS enrolled_count
                    FROM registrations
                    WHERE status = 'ENROLLED'
                    GROUP BY course_code
                ) r ON r.course_code = c.course_code
                LEFT JOIN (
                    SELECT course_code, COUNT(*) AS waitlist_count
                    FROM waitlist_entries
                    GROUP BY course_code
                ) w ON w.course_code = c.course_code
                ORDER BY c.course_code
                """;

        List<CourseEnrollmentReport> reports = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                int capacity = result.getInt("capacity");
                long enrolled = result.getLong("enrolled_count");
                long waiting = result.getLong("waitlist_count");

                reports.add(new CourseEnrollmentReport(
                        result.getString("course_code"),
                        result.getString("title"),
                        capacity,
                        result.getString("status"),
                        enrolled,
                        waiting,
                        Math.max(0L, capacity - enrolled)
                ));
            }
        }

        return List.copyOf(reports);
    }

    public List<StudentRegistrationReport> findStudentRegistrations(
            Connection connection
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection");

        String sql = """
                SELECT
                    s.student_id,
                    c.course_code,
                    c.title,
                    r.status,
                    r.created_at
                FROM registrations r
                INNER JOIN students s
                    ON s.student_id = r.student_id
                INNER JOIN courses c
                    ON c.course_code = r.course_code
                ORDER BY s.student_id, r.created_at DESC
                """;

        List<StudentRegistrationReport> reports = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                Timestamp createdAt = result.getTimestamp("created_at");

                reports.add(new StudentRegistrationReport(
                        result.getString("student_id"),
                        result.getString("course_code"),
                        result.getString("title"),
                        result.getString("status"),
                        createdAt == null ? null : createdAt.toInstant()
                ));
            }
        }

        return List.copyOf(reports);
    }

    public List<CourseWaitlistReport> findCourseWaitlist(
            Connection connection
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection");

        String sql = """
                SELECT
                    c.course_code,
                    c.title,
                    s.student_id,
                    w.position,
                    w.created_at
                FROM waitlist_entries w
                INNER JOIN students s
                    ON s.student_id = w.student_id
                INNER JOIN courses c
                    ON c.course_code = w.course_code
                ORDER BY c.course_code, w.position
                """;

        List<CourseWaitlistReport> reports = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                Timestamp createdAt = result.getTimestamp("created_at");

                reports.add(new CourseWaitlistReport(
                        result.getString("course_code"),
                        result.getString("title"),
                        result.getString("student_id"),
                        result.getInt("position"),
                        createdAt == null ? null : createdAt.toInstant()
                ));
            }
        }

        return List.copyOf(reports);
    }
}
