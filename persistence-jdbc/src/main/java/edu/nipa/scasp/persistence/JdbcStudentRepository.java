package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.StudentRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcStudentRepository implements StudentRepository {
    private final DataSource dataSource;

    public JdbcStudentRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Student> findById(String id) {
        String sql = "SELECT student_id, user_id, programme, year_of_study "
                + "FROM students WHERE student_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find student", failure);
        }
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT student_id, user_id, programme, year_of_study "
                + "FROM students ORDER BY student_id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Student> students = new ArrayList<>();
            while (results.next()) {
                students.add(read(results));
            }
            return List.copyOf(students);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list students", failure);
        }
    }

    @Override
    public void save(String id, Student student) {
        if (!Objects.requireNonNull(id, "id").equals(student.studentId())) {
            throw new IllegalArgumentException("Repository key must match studentId");
        }
        String sql = "UPDATE students SET user_id = ?, programme = ?, year_of_study = ? "
                + "WHERE student_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            update.setString(1, student.userId());
            update.setString(2, student.programme());
            update.setInt(3, student.yearOfStudy());
            update.setString(4, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO students (student_id, user_id, programme, year_of_study) "
                                + "VALUES (?, ?, ?, ?)")) {
                    insert.setString(1, id);
                    insert.setString(2, student.userId());
                    insert.setString(3, student.programme());
                    insert.setInt(4, student.yearOfStudy());
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save student", failure);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM students WHERE student_id = ?")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete student", failure);
        }
    }

    private static Student read(ResultSet results) throws SQLException {
        return new Student(results.getString("student_id"), results.getString("user_id"),
                results.getString("programme"), results.getInt("year_of_study"));
    }
}
