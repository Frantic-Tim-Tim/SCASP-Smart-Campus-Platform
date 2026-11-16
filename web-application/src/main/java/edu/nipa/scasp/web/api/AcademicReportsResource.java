package edu.nipa.scasp.web.api;

import java.security.Principal;

import edu.nipa.scasp.enterprise.service.AcademicReportsService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/admin/reports")
@Produces(MediaType.APPLICATION_JSON)
public class AcademicReportsResource {

    @EJB
    private AcademicReportsService academicReportsService;

    @Context
    private SecurityContext securityContext;

    @GET
    @Path("/course-enrolment")
    public Response courseEnrolmentSummary() {
        return executeReport(
                () -> academicReportsService.getCourseEnrollmentSummary(
                        authenticatedUserId()
                )
        );
    }

    @GET
    @Path("/student-registrations")
    public Response studentRegistrations() {
        return executeReport(
                () -> academicReportsService.getStudentRegistrations(
                        authenticatedUserId()
                )
        );
    }

    @GET
    @Path("/course-waitlist")
    public Response courseWaitlist() {
        return executeReport(
                () -> academicReportsService.getCourseWaitlist(
                        authenticatedUserId()
                )
        );
    }

    private String authenticatedUserId() {
        Principal principal = securityContext == null
                ? null
                : securityContext.getUserPrincipal();

        if (principal == null || principal.getName() == null
                || principal.getName().isBlank()) {
            throw new UnauthenticatedException();
        }

        return principal.getName();
    }

    private Response executeReport(ReportOperation operation) {
        try {
            return Response.ok(operation.execute()).build();
        } catch (UnauthenticatedException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Authentication is required"))
                    .build();
        } catch (SecurityException exception) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("Administrator access is required"))
                    .build();
        } catch (Exception exception) {
            return Response.serverError()
                    .entity(new ErrorResponse("The report could not be generated"))
                    .build();
        }
    }

    @FunctionalInterface
    private interface ReportOperation {
        Object execute();
    }

    private static final class UnauthenticatedException
            extends RuntimeException {
    }

    public record ErrorResponse(String message) {
    }
}
