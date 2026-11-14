package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import edu.nipa.scasp.domain.Student;

public final class JdbcStudentRepository implements StudentRepository {

    private final ConnectionFactory connectionFactory;

    public JdbcStudentRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory"
        );
    }

    @Override
    public Optional<Student> findByUserId(String userId)
            throws SQLException {

        String sql = """
                SELECT student_id, user_id, programme, year_of_study
                FROM students
                WHERE user_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapStudent(result));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Student> findByStudentId(String studentId)
            throws SQLException {

        String sql = """
                SELECT student_id, user_id, programme, year_of_study
                FROM students
                WHERE student_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapStudent(result));
                }
            }
        }

        return Optional.empty();
    }

    private Student mapStudent(ResultSet result) throws SQLException {
        return new Student(
                result.getString("student_id"),
                result.getString("user_id"),
                result.getString("programme"),
                result.getInt("year_of_study")
        );
    }
}
