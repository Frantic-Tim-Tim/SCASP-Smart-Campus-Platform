package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;

public interface UserManagementService {

    User registerUser(User actor, User newUser);

    User getUserById(String userId);

    List<User> getAllUsers(User actor);

    User changeUserRole(
            User actor,
            String userId,
            UserRole newRole
    );

    User suspendUser(User actor, String userId);

    User activateUser(User actor, String userId);
}