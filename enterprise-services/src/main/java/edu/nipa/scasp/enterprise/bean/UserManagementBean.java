package edu.nipa.scasp.enterprise.bean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.UserRepository;
import edu.nipa.scasp.enterprise.service.UserManagementService;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(UserManagementService.class)
public class UserManagementBean implements UserManagementService {

    @EJB
    private UserRepository userRepository;

    // Used by unit tests; the EJB container uses the no-argument constructor.
    public UserManagementBean() {
    }

    UserManagementBean(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    public User registerUser(User actor, User newUser) {
        requireAdministrator(actor);
        Objects.requireNonNull(newUser, "newUser");

        if (userRepository.existsById(newUser.userId())) {
            throw new IllegalArgumentException(
                    "A user with ID " + newUser.userId() + " already exists"
            );
        }

        return userRepository.save(newUser);
    }

    @Override
    public User getUserById(String userId) {
        requireText(userId, "userId");

        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found: " + userId
                ));
    }

    @Override
    public List<User> getAllUsers(User actor) {
        requireAdministrator(actor);
        return List.copyOf(userRepository.findAll());
    }

    @Override
    public User changeUserRole(
            User actor,
            String userId,
            UserRole newRole
    ) {
        requireAdministrator(actor);
        requireText(userId, "userId");
        Objects.requireNonNull(newRole, "newRole");

        User existing = getUserById(userId);

        if (existing.role() == UserRole.ADMIN
                && newRole != UserRole.ADMIN
                && existing.status() == UserStatus.ACTIVE) {

            preventSelfRemoval(actor, existing);
            ensureAnotherActiveAdministratorExists(existing);
        }

        User updated = new User(
                existing.userId(),
                existing.name(),
                newRole,
                existing.status()
        );

        return userRepository.save(updated);
    }

    @Override
    public User suspendUser(User actor, String userId) {
        return updateStatus(actor, userId, UserStatus.SUSPENDED);
    }

    @Override
    public User activateUser(User actor, String userId) {
        return updateStatus(actor, userId, UserStatus.ACTIVE);
    }

    private User updateStatus(
            User actor,
            String userId,
            UserStatus newStatus
    ) {
        requireAdministrator(actor);
        requireText(userId, "userId");

        User existing = getUserById(userId);

        if (existing.role() == UserRole.ADMIN
                && existing.status() == UserStatus.ACTIVE
                && newStatus == UserStatus.SUSPENDED) {

            preventSelfRemoval(actor, existing);
            ensureAnotherActiveAdministratorExists(existing);
        }

        User updated = new User(
                existing.userId(),
                existing.name(),
                existing.role(),
                newStatus
        );

        return userRepository.save(updated);
    }

    private void preventSelfRemoval(User actor, User target) {
        if (actor.userId().equals(target.userId())) {
            throw new SecurityException(
                    "Administrators cannot demote or suspend their own accounts"
            );
        }
    }

    private void ensureAnotherActiveAdministratorExists(User target) {
        long activeAdministratorCount = userRepository.findAll()
                .stream()
                .filter(user -> user.role() == UserRole.ADMIN)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .count();

        if (activeAdministratorCount <= 1) {
            throw new IllegalStateException(
                    "The last active administrator cannot be demoted or suspended"
            );
        }
    }

    private void requireAdministrator(User actor) {
        Objects.requireNonNull(actor, "actor");

        if (actor.role() != UserRole.ADMIN) {
            throw new SecurityException(
                    "Only administrators can perform this operation"
            );
        }

        if (actor.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "The administrator account is not active"
            );
        }
    }

    private void requireText(String value, String field) {
        Objects.requireNonNull(value, field);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
    }
}
