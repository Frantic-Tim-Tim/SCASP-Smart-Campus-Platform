package edu.nipa.scasp.enterprise.bean;

import java.security.Principal;
import java.util.List;

import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.enterprise.service.RemoteUserManagementService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
@DeclareRoles("ADMIN")
@RolesAllowed("ADMIN")
public class RemoteUserManagementBean
        implements RemoteUserManagementService {

    @EJB
    private UserManagementService userManagementService;

    @Resource
    private SessionContext sessionContext;

    /**
     * Gets the authenticated administrator from WildFly.
     * The identity comes from the application security context,
     * not from data supplied by the remote client.
     */
    private User getAuthenticatedAdministrator() {

        Principal principal = sessionContext.getCallerPrincipal();

        if (principal == null) {
            throw new SecurityException(
                    "No authenticated user was found."
            );
        }

        String userId = principal.getName();

        User actor = userManagementService.getUserById(userId);

        if (actor == null
            || actor.role() != UserRole.ADMIN
            || actor.status() != UserStatus.ACTIVE) {

            throw new SecurityException(
                    "Only active administrators can perform this operation."
            );
        }

        return actor;
    }

    @Override
    public User registerUser(User newUser) {

        User actor = getAuthenticatedAdministrator();

        return userManagementService.registerUser(actor, newUser);
    }

    @Override
    public User getUserById(String userId) {

        getAuthenticatedAdministrator();

        return userManagementService.getUserById(userId);
    }

    @Override
    public List<User> getAllUsers() {

        User actor = getAuthenticatedAdministrator();

        return userManagementService.getAllUsers(actor);
    }

    @Override
    public User changeUserRole(String userId, UserRole newRole) {

        User actor = getAuthenticatedAdministrator();

        return userManagementService.changeUserRole(
                actor, userId, newRole
        );
    }

    @Override
    public User suspendUser(String userId) {

        User actor = getAuthenticatedAdministrator();

        return userManagementService.suspendUser(actor, userId);
    }

    @Override
    public User activateUser(String userId) {

        User actor = getAuthenticatedAdministrator();

        return userManagementService.activateUser(actor, userId);
    }
}