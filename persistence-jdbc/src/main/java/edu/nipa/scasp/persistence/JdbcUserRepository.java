package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;

public final class JdbcUserRepository implements UserRepository {

    private final ConnectionFactory connectionFactory;

    public JdbcUserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory"
        );
    }

    @Override
    public void save(User user) throws SQLException {
        String sql = """
                INSERT INTO users (user_id, name, role, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.userId());
            statement.setString(2, user.name());
            statement.setString(3, user.role().name());
            statement.setString(4, user.status().name());

            statement.executeUpdate();
        }
    }

    @Override
    public Optional<User> findById(String userId) throws SQLException {
        String sql = """
                SELECT user_id, name, role, status
                FROM users
                WHERE user_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapUser(result));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = """
                SELECT user_id, name, role, status
                FROM users
                ORDER BY user_id
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }
        }

        return users;
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = """
                UPDATE users
                SET name = ?, role = ?, status = ?
                WHERE user_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.name());
            statement.setString(2, user.role().name());
            statement.setString(3, user.status().name());
            statement.setString(4, user.userId());

            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            return statement.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getString("user_id"),
                result.getString("name"),
                UserRole.valueOf(result.getString("role")),
                UserStatus.valueOf(result.getString("status"))
        );
    }
}