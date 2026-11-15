package edu.nipa.scasp.web;

import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.service.StudentAccountProvisioningService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

    @EJB
    private StudentAccountProvisioningService studentAccountProvisioningService;

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

        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();

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

        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();

        } catch (SecurityException exception) {
            return forbidden();
        }
    }

    /**
     * Creates a student account, including the user record,
     * password credentials and student profile.
     *
     * Endpoint:
     * POST /api/admin/users/students
     */
    @POST
    @Path("students")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createStudentAccount(
            Map<String, Object> payload,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        if (payload == null) {
            return badRequest("Student account details are required.");
        }

        char[] password = null;

        try {
            String userId = requiredString(payload, "userId");
            String name = requiredString(payload, "name");
            String studentId = requiredString(payload, "studentId");
            String programme = requiredString(payload, "programme");

            int yearOfStudy = requiredInteger(payload, "yearOfStudy");

            // Do not trim passwords. Spaces may be intentional.
            Object passwordValue = payload.get("password");

            if (!(passwordValue instanceof String passwordText)
                    || passwordText.isBlank()) {
                return badRequest("A valid password is required.");
            }

            password = passwordText.toCharArray();

            Student student =
                    studentAccountProvisioningService.createStudentAccount(
                            actor,
                            userId,
                            name,
                            studentId,
                            programme,
                            yearOfStudy,
                            password
                    );

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "userId", student.userId(),
                            "studentId", student.studentId(),
                            "programme", student.programme(),
                            "yearOfStudy", student.yearOfStudy(),
                            "role", UserRole.STUDENT.name(),
                            "status", UserStatus.ACTIVE.name()
                    ))
                    .build();

        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());

        } catch (SecurityException exception) {
            return forbidden();

        } catch (NoSuchElementException exception) {
            // The administrator account may no longer exist.
            return forbidden();

        } catch (EJBException exception) {
            if (containsConstraintViolation(exception)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of(
                                "error",
                                "A user ID or student ID already exists."
                        ))
                        .build();
            }

            return Response.serverError()
                    .entity(Map.of(
                            "error",
                            "The student account could not be created."
                    ))
                    .build();

        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private String requiredString(
            Map<String, Object> payload,
            String field) {

        Object value = payload.get(field);

        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "A valid " + field + " is required."
            );
        }

        return text.trim();
    }

    private int requiredInteger(
            Map<String, Object> payload,
            String field) {

        Object value = payload.get(field);

        if (value instanceof Number number) {
            double numericValue = number.doubleValue();

            if (!Double.isFinite(numericValue)
                    || numericValue != Math.floor(numericValue)
                    || numericValue < Integer.MIN_VALUE
                    || numericValue > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "A valid " + field + " is required."
                );
            }

            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());

            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "A valid " + field + " is required."
                );
            }
        }

        throw new IllegalArgumentException(
                "A valid " + field + " is required."
        );
    }

    private boolean containsConstraintViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();

                if (sqlState != null && sqlState.startsWith("23")) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private User getActor(SecurityContext securityContext) {

        if (securityContext == null) {
            return null;
        }

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
                .entity(Map.of(
                        "error",
                        "Authentication is required."
                ))
                .build();
    }

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of(
                        "error",
                        "You are not authorised."
                ))
                .build();
    }

    private Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "error",
                        "User not found."
                ))
                .build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", message))
                .build();
    }
}
