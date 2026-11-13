package edu.nipa.scasp.enterprise.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.UserRepository;

class UserManagementBeanTest {

    private FakeUserRepository repository;
    private UserManagementBean service;

    private final User admin = new User(
            "ADM001",
            "System Administrator",
            UserRole.ADMIN,
            UserStatus.ACTIVE
    );

    private final User student = new User(
            "STU001",
            "John Student",
            UserRole.STUDENT,
            UserStatus.ACTIVE
    );

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        service = new UserManagementBean(repository);
    }

    @Test
    void administratorCanRegisterUser() {
        User result = service.registerUser(admin, student);

        assertEquals("STU001", result.userId());
        assertTrue(repository.existsById("STU001"));
    }

    @Test
    void duplicateUserCannotBeRegistered() {
        service.registerUser(admin, student);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerUser(admin, student)
        );
    }

    @Test
    void userCanBeRetrievedById() {
        service.registerUser(admin, student);

        User result = service.getUserById("STU001");

        assertEquals(student, result);
    }

    @Test
    void missingUserThrowsException() {
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> service.getUserById("UNKNOWN")
        );
    }

    @Test
    void administratorCanChangeUserRole() {
        service.registerUser(admin, student);

        User updated = service.changeUserRole(
                admin,
                "STU001",
                UserRole.STAFF
        );

        assertEquals(UserRole.STAFF, updated.role());
    }

    @Test
    void administratorCanSuspendUser() {
        service.registerUser(admin, student);

        User updated = service.suspendUser(admin, "STU001");

        assertEquals(UserStatus.SUSPENDED, updated.status());
    }

    @Test
    void administratorCanActivateUser() {
        service.registerUser(admin, student);
        service.suspendUser(admin, "STU001");

        User updated = service.activateUser(admin, "STU001");

        assertEquals(UserStatus.ACTIVE, updated.status());
    }

    @Test
    void nonAdministratorCannotRegisterUser() {
        assertThrows(
                SecurityException.class,
                () -> service.registerUser(student, new User(
                        "STU002",
                        "Another Student",
                        UserRole.STUDENT,
                        UserStatus.ACTIVE
                ))
        );
    }

    @Test
    void suspendedAdministratorCannotManageUsers() {
        User suspendedAdmin = new User(
                "ADM002",
                "Suspended Admin",
                UserRole.ADMIN,
                UserStatus.SUSPENDED
        );

        assertThrows(
                SecurityException.class,
                () -> service.getAllUsers(suspendedAdmin)
        );
    }

    @Test
    void administratorCannotDemoteOwnAccount() {
        repository.save(admin);

        assertThrows(
                SecurityException.class,
                () -> service.changeUserRole(
                        admin,
                        admin.userId(),
                        UserRole.STAFF
                )
        );

        assertEquals(
                UserRole.ADMIN,
                repository.findById(admin.userId()).orElseThrow().role()
        );
    }

    @Test
    void administratorCannotSuspendOwnAccount() {
        repository.save(admin);

        assertThrows(
                SecurityException.class,
                () -> service.suspendUser(admin, admin.userId())
        );

        assertEquals(
                UserStatus.ACTIVE,
                repository.findById(admin.userId()).orElseThrow().status()
        );
    }

    
    @Test
    void administratorCannotDemoteOwnAccountWhenItIsTheOnlyAdmin() {
        repository.save(admin);

        assertThrows(
                SecurityException.class,
                () -> service.changeUserRole(
                        admin,
                        admin.userId(),
                        UserRole.STAFF
                )
        );

        assertEquals(
                UserRole.ADMIN,
                repository.findById("ADM001").orElseThrow().role()
        );
    }

    @Test
    void administratorCannotSuspendOwnAccountWhenItIsTheOnlyAdmin() {
        repository.save(admin);

        assertThrows(
                SecurityException.class,
                () -> service.suspendUser(admin, "ADM001")
        );

        assertEquals(
                UserStatus.ACTIVE,
                repository.findById("ADM001").orElseThrow().status()
        );
    }

    @Test
    void administratorCanDemoteAnotherAdministratorWhenOneRemains() {
        repository.save(admin);

        User secondAdmin = new User(
                "ADM002",
                "Second Administrator",
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );

        repository.save(secondAdmin);

        User updated = service.changeUserRole(
                admin,
                "ADM002",
                UserRole.STAFF
        );

        assertEquals(UserRole.STAFF, updated.role());
        assertEquals(
                UserRole.ADMIN,
                repository.findById("ADM001").orElseThrow().role()
        );
    }


    @Test
    void administratorCanSuspendAnotherAdministratorWhenOneRemains() {
        repository.save(admin);

        User secondAdmin = new User(
                "ADM002",
                "Second Administrator",
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );

        repository.save(secondAdmin);

        User updated = service.suspendUser(admin, "ADM002");

        assertEquals(UserStatus.SUSPENDED, updated.status());
        assertEquals(
                UserStatus.ACTIVE,
                repository.findById("ADM001").orElseThrow().status()
        );
    }


    private static class FakeUserRepository implements UserRepository {

        private final List<User> users = new ArrayList<>();

        @Override
        public Optional<User> findById(String userId) {
            return users.stream()
                    .filter(user -> user.userId().equals(userId))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }

        @Override
        public boolean existsById(String userId) {
            return findById(userId).isPresent();
        }

        @Override
        public User save(User user) {
            users.removeIf(
                    existing -> existing.userId().equals(user.userId())
            );

            users.add(user);
            return user;
        }
    }
}