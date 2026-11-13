package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import jakarta.ejb.Remote;

@Remote
public interface RemoteUserManagementService {

    User registerUser(User newUser);

    User getUserById(String userId);

    List<User> getAllUsers();

    User changeUserRole(String userId, UserRole newRole);

    User suspendUser(String userId);

    User activateUser(String userId);
}