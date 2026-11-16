package edu.nipa.scasp.web.resource;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.enterprise.service.ServiceRequestService;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;
import java.util.function.Supplier;

@Path("/service-requests")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceRequestResource {

    @EJB
    private ServiceRequestService service;

    @Context
    private SecurityContext securityContext;

    public static class CreateRequest {
        public String title;
        public String description;
        public String category;
        public RequestPriority priority;
    }

    public static class AssignRequest {
        public String staffUserId;
    }

    public static class PriorityRequest {
        public RequestPriority priority;
    }

    public static class StatusRequest {
        public RequestStatus status;
    }

    public static class ProgressRequest {
        public String note;
    }

    @POST
    public Response submit(CreateRequest body) {
        return respond(() -> {
            requireBody(body);

            return service.submit(
                    authenticatedUserId(),
                    body.title,
                    body.description,
                    body.category,
                    body.priority
            );
        }, Response.Status.CREATED);
    }

    @GET
    @Path("/mine")
    public Response getMyRequests() {
        return respond(
                () -> service.getMyRequests(authenticatedUserId()),
                Response.Status.OK
        );
    }

    @GET
    public Response getAllRequests() {
        return respond(
                () -> service.getAllRequests(authenticatedUserId()),
                Response.Status.OK
        );
    }

    @GET
    @Path("/{requestId}")
    public Response getRequest(@PathParam("requestId") UUID requestId) {
        return respond(
                () -> service.getMyRequest(
                        authenticatedUserId(), requestId
                ),
                Response.Status.OK
        );
    }

    @POST
    @Path("/{requestId}/assign")
    public Response assign(
            @PathParam("requestId") UUID requestId,
            AssignRequest body
    ) {
        return respond(() -> {
            requireBody(body);

            return service.assign(
                    authenticatedUserId(),
                    requestId,
                    body.staffUserId
            );
        }, Response.Status.OK);
    }

    @PATCH
    @Path("/{requestId}/priority")
    public Response changePriority(
            @PathParam("requestId") UUID requestId,
            PriorityRequest body
    ) {
        return respond(() -> {
            requireBody(body);

            return service.changePriority(
                    authenticatedUserId(),
                    requestId,
                    body.priority
            );
        }, Response.Status.OK);
    }

    @PATCH
    @Path("/{requestId}/status")
    public Response changeStatus(
            @PathParam("requestId") UUID requestId,
            StatusRequest body
    ) {
        return respond(() -> {
            requireBody(body);

            return service.changeStatus(
                    authenticatedUserId(),
                    requestId,
                    body.status
            );
        }, Response.Status.OK);
    }

    @POST
    @Path("/{requestId}/updates")
    public Response addProgressUpdate(
            @PathParam("requestId") UUID requestId,
            ProgressRequest body
    ) {
        return respond(() -> {
            requireBody(body);

            return service.addProgressUpdate(
                    authenticatedUserId(),
                    requestId,
                    body.note
            );
        }, Response.Status.CREATED);
    }

    @GET
    @Path("/{requestId}/updates")
    public Response getUpdates(
            @PathParam("requestId") UUID requestId
    ) {
        return respond(
                () -> service.getUpdates(
                        authenticatedUserId(), requestId
                ),
                Response.Status.OK
        );
    }

    private String authenticatedUserId() {
        if (securityContext == null
                || securityContext.getUserPrincipal() == null) {
            throw new SecurityException("Authentication is required");
        }

        return securityContext.getUserPrincipal().getName();
    }

    private void requireBody(Object body) {
        if (body == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }
    }

    private Response respond(
            Supplier<Object> operation,
            Response.Status successStatus
    ) {
        try {
            return Response.status(successStatus)
                    .entity(operation.get())
                    .build();
        } catch (SecurityException exception) {
            return error(Response.Status.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(Response.Status.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return error(Response.Status.CONFLICT, exception.getMessage());
        } catch (java.util.NoSuchElementException exception) {
            return error(Response.Status.NOT_FOUND, exception.getMessage());
        } catch (EJBException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof SecurityException) {
                return error(Response.Status.FORBIDDEN, cause.getMessage());
            }
            if (cause instanceof IllegalArgumentException) {
                return error(Response.Status.BAD_REQUEST, cause.getMessage());
            }
            if (cause instanceof IllegalStateException) {
                return error(Response.Status.CONFLICT, cause.getMessage());
            }
            if (cause instanceof java.util.NoSuchElementException) {
                return error(Response.Status.NOT_FOUND, cause.getMessage());
            }

            return error(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "A service request operation failed"
            );
        } catch (RuntimeException exception) {
            return error(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "An unexpected service request error occurred"
            );
        }
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .build();
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
