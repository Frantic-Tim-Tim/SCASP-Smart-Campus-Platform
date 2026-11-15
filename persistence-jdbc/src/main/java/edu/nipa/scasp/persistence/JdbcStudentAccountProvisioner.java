package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;

/**
 * Creates a student account and its related database records.
 *
 * All operations use the same connection so the calling
 * transaction can commit or roll back the complete operation.
 */
public final class JdbcStudentAccountProvisioner {

    public Student create(
            Connection connection,
            User user,
            Student student,
            String passwordHash
    ) throws SQLException {

        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(student, "student");

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Password hash must not be blank"
            );
        }

        if (user.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException(
                    "The account must have the STUDENT role"
            );
        }

        if (user.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "A new student account must be ACTIVE"
            );
        }

        if (!user.userId().equals(student.userId())) {
            throw new IllegalArgumentException(
                    "User ID and student profile user ID must match"
            );
        }

        insertUser(connection, user);
        insertCredential(connection, user.userId(), passwordHash);
        insertStudent(connection, student);

        return student;
    }

    private void insertUser(
            Connection connection,
            User user
    ) throws SQLException {

        String sql = """
                INSERT INTO users (user_id, name, role, status)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, user.userId());
            statement.setString(2, user.name());
            statement.setString(3, user.role().name());
            statement.setString(4, user.status().name());

            statement.executeUpdate();
        }
    }

    private void insertCredential(
            Connection connection,
            String userId,
            String passwordHash
    ) throws SQLException {

        String sql = """
                INSERT INTO user_credentials (user_id, password_hash)
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, userId);
            statement.setString(2, passwordHash);

            statement.executeUpdate();
        }
    }

    private void insertStudent(
            Connection connection,
            Student student
    ) throws SQLException {

        String sql = """
                INSERT INTO students
                    (student_id, user_id, programme, year_of_study)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, student.studentId());
            statement.setString(2, student.userId());
            statement.setString(3, student.programme());
            statement.setInt(4, student.yearOfStudy());

            statement.executeUpdate();
        }
    }
}
