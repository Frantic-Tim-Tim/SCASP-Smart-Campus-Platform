package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseRepository;
import edu.nipa.scasp.domain.CourseStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcCourseRepository implements CourseRepository {
    private final DataSource dataSource;

    public JdbcCourseRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Course> findById(String id) {
        String sql = "SELECT course_code, title, capacity, status FROM courses WHERE course_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find course", failure);
        }
    }

    @Override
    public List<Course> findAll() {
        return findAll(Comparator.comparing(Course::courseCode));
    }

    @Override
    public List<Course> findAll(Comparator<Course> ordering) {
        String sql = "SELECT course_code, title, capacity, status FROM courses";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            java.util.ArrayList<Course> courses = new java.util.ArrayList<>();
            while (results.next()) {
                courses.add(read(results));
            }
            return courses.stream().sorted(Objects.requireNonNull(ordering, "ordering")).toList();
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list courses", failure);
        }
    }

    @Override
    public List<Course> search(String query) {
        String sql = "SELECT course_code, title, capacity, status FROM courses "
                + "WHERE LOWER(course_code) LIKE ? OR LOWER(title) LIKE ? ORDER BY course_code";
        String term = "%" + Objects.requireNonNull(query, "query").toLowerCase(java.util.Locale.ROOT)
                + "%";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, term);
            statement.setString(2, term);
            try (ResultSet results = statement.executeQuery()) {
                java.util.ArrayList<Course> courses = new java.util.ArrayList<>();
                while (results.next()) {
                    courses.add(read(results));
                }
                return List.copyOf(courses);
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("search courses", failure);
        }
    }

    @Override
    public void save(String id, Course course) {
        if (!Objects.requireNonNull(id, "id").equals(course.courseCode())) {
            throw new IllegalArgumentException("Repository key must match courseCode");
        }
        String update = "UPDATE courses SET title = ?, capacity = ?, status = ? WHERE course_code = ?";
        String insert = "INSERT INTO courses (title, capacity, status, course_code) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, course.title());
            statement.setInt(2, course.capacity());
            statement.setString(3, course.status().name());
            statement.setString(4, id);
            if (statement.executeUpdate() == 0) {
                try (PreparedStatement insertion = connection.prepareStatement(insert)) {
                    insertion.setString(1, course.title());
                    insertion.setInt(2, course.capacity());
                    insertion.setString(3, course.status().name());
                    insertion.setString(4, id);
                    insertion.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save course", failure);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM courses WHERE course_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete course", failure);
        }
    }

    private static Course read(ResultSet results) throws SQLException {
        return new Course(results.getString("course_code"), results.getString("title"),
                results.getInt("capacity"),
                CourseStatus.valueOf(results.getString("status")));
    }
}
