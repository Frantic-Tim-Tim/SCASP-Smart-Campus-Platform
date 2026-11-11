package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcUserRepository implements edu.nipa.scasp.domain.Repository<String, User> {
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<User> findById(String id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT user_id, display_name, role, status FROM users WHERE user_id = ?")) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(read(results)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("find user", failure);
        }
    }

    @Override
    public List<User> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT user_id, display_name, role, status FROM users ORDER BY user_id");
             ResultSet results = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (results.next()) {
                users.add(read(results));
            }
            return List.copyOf(users);
        } catch (SQLException failure) {
            throw JdbcSupport.failure("list users", failure);
        }
    }

    @Override
    public void save(String id, User user) {
        if (!Objects.requireNonNull(id, "id").equals(user.userId())) {
            throw new IllegalArgumentException("Repository key must match userId");
        }
        String updateSql = "UPDATE users SET display_name = ?, role = ?, status = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            update.setString(1, user.name());
            update.setString(2, user.role().name());
            update.setString(3, user.status().name());
            update.setString(4, id);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users (display_name, role, status, user_id) VALUES (?, ?, ?, ?)")) {
                    insert.setString(1, user.name());
                    insert.setString(2, user.role().name());
                    insert.setString(3, user.status().name());
                    insert.setString(4, id);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("save user", failure);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM users WHERE user_id = ?")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException failure) {
            throw JdbcSupport.failure("delete user", failure);
        }
    }

    private static User read(ResultSet results) throws SQLException {
        return new User(results.getString("user_id"), results.getString("display_name"),
                UserRole.valueOf(results.getString("role")),
                UserStatus.valueOf(results.getString("status")));
    }
}
