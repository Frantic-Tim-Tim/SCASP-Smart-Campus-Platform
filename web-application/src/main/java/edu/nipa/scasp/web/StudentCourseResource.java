package edu.nipa.scasp.web;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.enterprise.service.StudentCourseCatalogueService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("student/courses")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("STUDENT")
public class StudentCourseResource {

    private static final Logger LOGGER =
            Logger.getLogger(StudentCourseResource.class.getName());

    @EJB
    private StudentCourseCatalogueService catalogueService;

    @Context
    private SecurityContext securityContext;

    @GET
    public Response getAvailableCourses(
            @QueryParam("search") String search) {

        if (securityContext == null
                || securityContext.getUserPrincipal() == null) {

            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                            "error", "Authentication is required"))
                    .build();
        }

        String userId = securityContext.getUserPrincipal().getName();

        try {
            List<Course> courses =
                    catalogueService.getAvailableCourses(userId, search);

            return Response.ok(Map.of(
                    "courses", courses,
                    "count", courses.size()
            )).build();

        } catch (SecurityException exception) {

            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();

        } catch (Exception exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Unable to retrieve course catalogue for user " + userId,
                    exception
            );

            return Response.serverError()
                    .entity(Map.of(
                            "error", "Unable to retrieve the course catalogue"))
                    .build();
        }
    }
}