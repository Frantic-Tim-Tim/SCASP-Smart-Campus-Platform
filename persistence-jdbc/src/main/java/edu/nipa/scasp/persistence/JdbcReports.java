package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

public final class JdbcReports {
    private final DataSource dataSource;

    public JdbcReports(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public List<CourseCapacity> courseCapacity() {
        String sql = "SELECT c.course_code, c.title, c.capacity, "
                + "COUNT(DISTINCT CASE WHEN r.status = 'ENROLLED' THEN r.student_id END) enrolled, "
                + "COUNT(DISTINCT w.entry_id) waiting "
                + "FROM courses c LEFT JOIN registrations r ON r.course_code = c.course_code "
                + "LEFT JOIN waitlist_entries w ON w.course_code = c.course_code "
                + "GROUP BY c.course_code, c.title, c.capacity ORDER BY c.course_code";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<CourseCapacity> report = new ArrayList<>();
            while (results.next()) {
                int capacity = results.getInt("capacity");
                int enrolled = results.getInt("enrolled");
                report.add(new CourseCapacity(results.getString("course_code"),
                        results.getString("title"), capacity, enrolled,
                        capacity - enrolled, results.getInt("waiting")));
            }
            return List.copyOf(report);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("course capacity report", failure);
        }
    }

    public List<StudentRegistration> studentRegistrations() {
        String sql = "SELECT s.student_id, u.display_name, s.programme, c.course_code, c.title, r.status "
                + "FROM students s JOIN users u ON u.user_id = s.user_id "
                + "LEFT JOIN registrations r ON r.student_id = s.student_id "
                + "LEFT JOIN courses c ON c.course_code = r.course_code "
                + "ORDER BY s.student_id, c.course_code";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<StudentRegistration> report = new ArrayList<>();
            while (results.next()) {
                report.add(new StudentRegistration(results.getString("student_id"),
                        results.getString("display_name"), results.getString("programme"),
                        results.getString("course_code"), results.getString("title"),
                        results.getString("status")));
            }
            return List.copyOf(report);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("student registration report", failure);
        }
    }

    public List<RequestWorkload> serviceRequestWorkload() {
        String sql = "SELECT u.user_id, u.display_name, r.status, r.priority, COUNT(*) request_count "
                + "FROM service_requests r LEFT JOIN users u ON u.user_id = r.assigned_to "
                + "GROUP BY u.user_id, u.display_name, r.status, r.priority "
                + "ORDER BY u.user_id, r.status, r.priority";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<RequestWorkload> report = new ArrayList<>();
            while (results.next()) {
                report.add(new RequestWorkload(results.getString("user_id"),
                        results.getString("display_name"), results.getString("status"),
                        results.getString("priority"), results.getLong("request_count")));
            }
            return List.copyOf(report);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("service request workload report", failure);
        }
    }

    public record CourseCapacity(String courseCode, String title, int capacity, int enrolled,
                                 int available, int waiting) {
    }

    public record StudentRegistration(String studentId, String studentName, String programme,
                                      String courseCode, String courseTitle, String status) {
    }

    public record RequestWorkload(String staffId, String staffName, String status,
                                  String priority, long requestCount) {
    }
}
