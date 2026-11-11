package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.PasswordHasher;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class JdbcAuthenticationService {
    private final DataSource dataSource;

    public JdbcAuthenticationService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public void register(User user, char[] password) {
        Objects.requireNonNull(user, "user");
        PasswordHasher.validate(password);
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash(password, salt);
        String sql = "INSERT INTO users (user_id, display_name, role, status, password_salt, "
                + "password_hash) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.userId());
            statement.setString(2, user.name());
            statement.setString(3, user.role().name());
            statement.setString(4, user.status().name());
            statement.setBytes(5, salt);
            statement.setBytes(6, hash);
            statement.executeUpdate();
        } catch (SQLException failure) {
            throw JdbcSupport.failure("register account", failure);
        }
    }

    public User authenticate(String userId, char[] password) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(password, "password");
        String sql = "SELECT display_name, role, status, password_salt, password_hash "
                + "FROM users WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SecurityException("Invalid credentials");
                }
                byte[] salt = result.getBytes("password_salt");
                byte[] hash = result.getBytes("password_hash");
                User user = new User(userId, result.getString("display_name"),
                        UserRole.valueOf(result.getString("role")),
                        UserStatus.valueOf(result.getString("status")));
                if (salt == null || hash == null || user.status() != UserStatus.ACTIVE
                        || !PasswordHasher.matches(password, salt, hash)) {
                    throw new SecurityException("Invalid credentials");
                }
                return user;
            }
        } catch (SQLException failure) {
            throw JdbcSupport.failure("authenticate account", failure);
        }
    }
}
