package edu.nipa.scasp.rmi.api;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.rmi.dto.ServerStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteAdminService extends Remote {

    // Existing monitoring operations
    String ping() throws RemoteException;

    ServerStatus getServerStatus() throws RemoteException;

    // Administrator authentication
    String login(String username, String password) throws RemoteException;

    void logout(String sessionToken) throws RemoteException;

    // User administration
    User registerUser(String sessionToken, User newUser)
            throws RemoteException;

    User getUserById(String sessionToken, String userId)
            throws RemoteException;

    List<User> getAllUsers(String sessionToken)
            throws RemoteException;

    User changeUserRole(
            String sessionToken, String userId, UserRole newRole)
            throws RemoteException;

    User suspendUser(String sessionToken, String userId)
            throws RemoteException;

    User activateUser(String sessionToken, String userId)
            throws RemoteException;
}