package edu.nipa.scasp.web;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import edu.nipa.scasp.enterprise.service.DuplicateCourseException;
import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.enterprise.service.CourseManagementService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("admin/courses")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class CourseResource {

    @EJB
    private CourseManagementService courseManagementService;

    @EJB
    private UserManagementService userManagementService;

    @GET
    public Response getCourses(
            @QueryParam("search") String search,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("direction") String direction,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            List<Map<String, Object>> courses =
                    courseManagementService.getCourses(
                            actor, search, sortBy, direction
                    ).stream()
                    .map(this::toResponse)
                    .toList();

            return Response.ok(courses).build();

        } catch (SecurityException exception) {
            return forbidden();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @GET
    @Path("{courseCode}")
    public Response getCourse(
            @PathParam("courseCode") String courseCode,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            Course course = courseManagementService.getCourse(
                    actor, courseCode
            );

            return Response.ok(toResponse(course)).build();

        } catch (NoSuchElementException exception) {
            return notFound("Course not found.");
        } catch (SecurityException exception) {
            return forbidden();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCourse(
            Map<String, Object> payload,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            Course course = toCourse(payload);

            Course created = courseManagementService.createCourse(
                    actor, course
            );

            return Response.status(Response.Status.CREATED)
                    .entity(toResponse(created))
                    .build();

        } catch (SecurityException exception) {
            return forbidden();
        } catch (DuplicateCourseException exception) {
            return conflict(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().startsWith("Course already exists")) {
                return conflict(exception.getMessage());
            }

            return badRequest(exception.getMessage());
        }
    }

    @PUT
    @Path("{courseCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateCourse(
            @PathParam("courseCode") String courseCode,
            Map<String, Object> payload,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            Course course = toCourse(payload);

            Course updated = courseManagementService.updateCourse(
                    actor, courseCode, course
            );

            return Response.ok(toResponse(updated)).build();

        } catch (NoSuchElementException exception) {
            return notFound("Course not found.");
        } catch (SecurityException exception) {
            return forbidden();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @DELETE
    @Path("{courseCode}")
    public Response deleteCourse(
            @PathParam("courseCode") String courseCode,
            @Context SecurityContext securityContext) {

        User actor = getActor(securityContext);

        if (actor == null) {
            return unauthorized();
        }

        try {
            courseManagementService.deleteCourse(actor, courseCode);
            return Response.noContent().build();

        } catch (NoSuchElementException exception) {
            return notFound("Course not found.");
        } catch (SecurityException exception) {
            return forbidden();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (IllegalStateException exception) {
            return conflict(exception.getMessage());
        }
    }

    private User getActor(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();

        if (principal == null) {
            return null;
        }

        try {
            return userManagementService.getUserById(principal.getName());
        } catch (NoSuchElementException exception) {
            return null;
        }
    }

    private Course toCourse(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("A course payload is required.");
        }

        Object code = payload.get("courseCode");
        Object title = payload.get("title");
        Object capacity = payload.get("capacity");
        Object status = payload.get("status");

        if (code == null || title == null || capacity == null || status == null) {
            throw new IllegalArgumentException(
                    "courseCode, title, capacity and status are required."
            );
        }

        int courseCapacity;

        try {
            courseCapacity = capacity instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(capacity.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Capacity must be a valid whole number."
            );
        }

        CourseStatus courseStatus;

        try {
            courseStatus = CourseStatus.valueOf(
                    status.toString().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Status must be OPEN or CLOSED."
            );
        }

        return new Course(
                code.toString(),
                title.toString(),
                courseCapacity,
                courseStatus
        );
    }

    private Map<String, Object> toResponse(Course course) {
        return Map.of(
                "courseCode", course.courseCode(),
                "title", course.title(),
                "capacity", course.capacity(),
                "status", course.status().name()
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

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", message))
                .build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", message == null
                        ? "Invalid request."
                        : message))
                .build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", message))
                .build();
    }
}
