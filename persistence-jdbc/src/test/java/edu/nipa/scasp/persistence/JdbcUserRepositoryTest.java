package edu.nipa.scasp.persistence;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;

class JdbcUserRepositoryTest {

    private JdbcUserRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:h2:mem:user_repository_test;MODE=MySQL;"
                        + "DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=TRUE",
                "sa",
                ""
        );

        ConnectionFactory factory = new ConnectionFactory(config);

        new DatabaseInitializer(factory).initialize();

        repository = new JdbcUserRepository(factory);
    }

    @Test
    void savesAndFindsUser() throws Exception {
        User user = new User(
                "STU001",
                "Thomas Chinga",
                UserRole.STUDENT,
                UserStatus.ACTIVE
        );

        repository.save(user);

        Optional<User> result = repository.findById("STU001");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void returnsEmptyWhenUserDoesNotExist() throws Exception {
        assertTrue(repository.findById("UNKNOWN").isEmpty());
    }

    @Test
    void updatesUser() throws Exception {
        User original = new User(
                "STU002", "Thomas", UserRole.STUDENT, UserStatus.ACTIVE
        );

        repository.save(original);

        User updated = new User(
                "STU002", "Thomas Chinga",
                UserRole.STUDENT, UserStatus.SUSPENDED
        );

        assertTrue(repository.update(updated));
        assertEquals(updated, repository.findById("STU002").orElseThrow());
    }

    @Test
    void deletesUser() throws Exception {
        User user = new User(
                "STU003", "Test User",
                UserRole.STAFF, UserStatus.ACTIVE
        );

        repository.save(user);

        assertTrue(repository.deleteById("STU003"));
        assertTrue(repository.findById("STU003").isEmpty());
    }
}