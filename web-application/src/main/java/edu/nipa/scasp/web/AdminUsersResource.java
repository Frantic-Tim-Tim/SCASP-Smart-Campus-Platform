package edu.nipa.scasp.web;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("admin/users")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class AdminUsersResource {

    @EJB
    private UserManagementService userManagementService;

    @GET
    public Response getAllUsers(
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            List<Map<String, String>> users =
                    userManagementService.getAllUsers(actor)
                            .stream()
                            .map(this::toResponse)
                            .toList();

            return Response.ok(users).build();

        } catch (SecurityException exception) {
            return forbidden();
        }
    }

    @PUT
    @Path("{userId}/role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserRole(
            @PathParam("userId") String userId,
            Map<String, String> payload,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        if (payload == null || payload.get("role") == null) {
            return badRequest("A role is required.");
        }

        try {
            UserRole newRole = UserRole.valueOf(
                    payload.get("role").trim().toUpperCase(Locale.ROOT)
            );

            User updated = userManagementService.changeUserRole(
                    actor,
                    userId,
                    newRole
            );

            return Response.ok(toResponse(updated)).build();

        } catch (NoSuchElementException exception) {
            return notFound();
        } catch (IllegalArgumentException exception) {
            return badRequest("Invalid user ID or role.");
        } catch (SecurityException exception) {
            return forbidden();
        }
    }

    @PUT
    @Path("{userId}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserStatus(
            @PathParam("userId") String userId,
            Map<String, String> payload,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        if (payload == null || payload.get("status") == null) {
            return badRequest("A status is required.");
        }

        try {
            UserStatus newStatus = UserStatus.valueOf(
                    payload.get("status").trim().toUpperCase(Locale.ROOT)
            );

            User updated;

            if (newStatus == UserStatus.ACTIVE) {
                updated = userManagementService.activateUser(actor, userId);
            } else {
                updated = userManagementService.suspendUser(actor, userId);
            }

            return Response.ok(toResponse(updated)).build();

        } catch (NoSuchElementException exception) {
            return notFound();
        } catch (IllegalArgumentException exception) {
            return badRequest("Invalid user ID or status.");
        } catch (SecurityException exception) {
            return forbidden();
        }
    }

    private User getActor(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();

        if (principal == null) {
            return null;
        }

        try {
            return userManagementService.getUserById(
                    principal.getName()
            );
        } catch (NoSuchElementException exception) {
            return null;
        }
    }

    private Map<String, String> toResponse(User user) {
        return Map.of(
                "userId", user.userId(),
                "name", user.name(),
                "role", user.role().name(),
                "status", user.status().name()
        );
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Authentication is required."))
                .build();
    }

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "You are not authorised."))
                .build();
    }

    private Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "User not found."))
                .build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", message))
                .build();
    }
}