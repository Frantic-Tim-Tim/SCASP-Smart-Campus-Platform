package edu.nipa.scasp.web;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.WaitlistEntry;
import edu.nipa.scasp.enterprise.service.CourseRegistrationService;
import edu.nipa.scasp.enterprise.service.RegistrationConflictException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("student/registrations")
@RolesAllowed("STUDENT")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StudentRegistrationResource {

    @Inject
    private CourseRegistrationService registrationService;

    @Context
    private SecurityContext securityContext;

    /**
     * Register the authenticated student for a course.
     *
     * POST /api/student/registrations/{courseCode}
     */
    @POST
    @Path("{courseCode}")
    public Response register(@PathParam("courseCode") String courseCode) {
        try {
            String userId = getAuthenticatedUserId();

            RegistrationAttempt attempt =
                    registrationService.registerForCourse(userId, courseCode);

            if (attempt.isWaitlisted()) {
                return Response.status(Response.Status.CREATED)
                        .entity(Map.of(
                                "status", "WAITLISTED",
                                "waitlistEntry", toWaitlistJson(
                                        attempt.waitlistEntry()
                                )
                        ))
                        .build();
            }

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "status", "ENROLLED",
                            "registration", toRegistrationJson(
                                    attempt.registration()
                            )
                    ))
                    .build();
        } catch (RegistrationConflictException e) {
            return conflict(e.getMessage());
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        }
    }

    /**
     * Withdraw the authenticated student from a course.
     *
     * DELETE /api/student/registrations/{courseCode}
     */
    @DELETE
    @Path("{courseCode}")
    public Response withdraw(@PathParam("courseCode") String courseCode) {
        try {
            String userId = getAuthenticatedUserId();

            Registration registration =
                    registrationService.withdrawFromCourse(userId, courseCode);

            return Response.ok(Map.of(
                    "message", "Registration withdrawn successfully",
                    "registration", toRegistrationJson(registration)
            )).build();

        } catch (RegistrationConflictException e) {
            return conflict(e.getMessage());
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        }
    }

    /**
     * Get the authenticated student's registrations.
     *
     * GET /api/student/registrations
     */
    @GET
    public Response getMyRegistrations() {
        try {
            String userId = getAuthenticatedUserId();

            List<Map<String, Object>> registrations =
                    registrationService.getMyRegistrations(userId)
                            .stream()
                            .map(StudentRegistrationResource::toRegistrationJson)
                            .toList();

            return Response.ok(Map.of(
                    "count", registrations.size(),
                    "registrations", registrations
            )).build();

        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        }
    }

    /**
     * Get the authenticated student's waitlist entries.
     *
     * GET /api/student/registrations/waitlist
     */
    @GET
    @Path("waitlist")
    public Response getMyWaitlistEntries() {
        try {
            String userId = getAuthenticatedUserId();

            List<Map<String, Object>> entries =
                    registrationService.getMyWaitlistEntries(userId)
                            .stream()
                            .map(StudentRegistrationResource::toWaitlistJson)
                            .toList();

            return Response.ok(Map.of(
                    "count", entries.size(),
                    "waitlistEntries", entries
            )).build();

        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        }
    }

    /**
     * Obtain the user ID from the authenticated session.
     * Student IDs are never accepted from the request.
     */
    private String getAuthenticatedUserId() {
        if (securityContext == null
                || securityContext.getUserPrincipal() == null) {
            throw new SecurityException("Authentication is required");
        }

        return securityContext.getUserPrincipal().getName();
    }

    private static Map<String, Object> toRegistrationJson(
            Registration registration
    ) {
        return Map.of(
                "registrationId", registration.registrationId().toString(),
                "studentId", registration.studentId(),
                "courseCode", registration.courseCode(),
                "status", registration.status().name(),
                "createdAt", registration.createdAt().toString()
        );
    }

    private static Map<String, Object> toWaitlistJson(
            WaitlistEntry entry
    ) {
        return Map.of(
                "entryId", entry.entryId().toString(),
                "studentId", entry.studentId(),
                "courseCode", entry.courseCode(),
                "position", entry.position(),
                "createdAt", entry.createdAt().toString()
        );
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", message))
                .build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", message))
                .build();
    }

    private Response forbidden(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", message))
                .build();
    }
}
