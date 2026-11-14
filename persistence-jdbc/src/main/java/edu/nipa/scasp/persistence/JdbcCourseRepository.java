package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;

public final class JdbcCourseRepository implements CourseRepository {

    private final ConnectionFactory connectionFactory;

    public JdbcCourseRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory"
        );
    }

    @Override
    public void save(Course course) throws SQLException {
        String sql = """
                INSERT INTO courses (course_code, title, capacity, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, course.courseCode());
            statement.setString(2, course.title());
            statement.setInt(3, course.capacity());
            statement.setString(4, course.status().name());

            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Course> findByCode(String courseCode)
            throws SQLException {

        String sql = """
                SELECT course_code, title, capacity, status
                FROM courses
                WHERE course_code = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapCourse(result));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<Course> findAll() throws SQLException {
        String sql = """
                SELECT course_code, title, capacity, status
                FROM courses
                ORDER BY course_code
                """;

        List<Course> courses = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                courses.add(mapCourse(result));
            }
        }

        return courses;
    }

    @Override
    public boolean update(Course course) throws SQLException {
        String sql = """
                UPDATE courses
                SET title = ?, capacity = ?, status = ?
                WHERE course_code = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, course.title());
            statement.setInt(2, course.capacity());
            statement.setString(3, course.status().name());
            statement.setString(4, course.courseCode());

            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteByCode(String courseCode) throws SQLException {
        String sql = "DELETE FROM courses WHERE course_code = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            return statement.executeUpdate() > 0;
        }
    }

    private Course mapCourse(ResultSet result) throws SQLException {
        return new Course(
                result.getString("course_code"),
                result.getString("title"),
                result.getInt("capacity"),
                CourseStatus.valueOf(result.getString("status"))
        );
    }
}
