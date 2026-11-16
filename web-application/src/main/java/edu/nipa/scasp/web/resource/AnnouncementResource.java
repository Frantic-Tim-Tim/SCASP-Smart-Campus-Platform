package edu.nipa.scasp.web.resource;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;
import edu.nipa.scasp.enterprise.service.AnnouncementService;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Path("/announcements")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AnnouncementResource {

    @EJB
    private AnnouncementService announcementService;

    @Context
    private SecurityContext securityContext;

    public static class AnnouncementRequest {
        public String title;
        public String body;
        public String audience;
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    @POST
    public Response publish(AnnouncementRequest request) {
        return respond(() -> {
            requireBody(request);

            Announcement announcement = announcementService.publish(
                    authenticatedUserId(),
                    request.title,
                    request.body,
                    parseAudience(request.audience)
            );

            return Response.status(Response.Status.CREATED)
                    .entity(announcement)
                    .build();
        });
    }

    @GET
    public Response getAll() {
        return respond(() -> announcementService.getAll(
                authenticatedUserId()
        ));
    }

    @GET
    @Path("/mine")
    public Response getMine() {
        return respond(() -> announcementService.getForUser(
                authenticatedUserId()
        ));
    }

    @PUT
    @Path("/{announcementId}")
    public Response revise(
            @PathParam("announcementId") String announcementId,
            AnnouncementRequest request
    ) {
        return respond(() -> {
            requireBody(request);

            return announcementService.revise(
                    authenticatedUserId(),
                    parseId(announcementId),
                    request.title,
                    request.body,
                    parseAudience(request.audience)
            );
        });
    }

    @PATCH
    @Path("/{announcementId}/archive")
    public Response archive(
            @PathParam("announcementId") String announcementId
    ) {
        return respond(() -> announcementService.archive(
                authenticatedUserId(),
                parseId(announcementId)
        ));
    }

    @POST
    @Path("/{announcementId}/read")
    public Response markRead(
            @PathParam("announcementId") String announcementId
    ) {
        return respond(() -> {
            announcementService.markRead(
                    authenticatedUserId(),
                    parseId(announcementId)
            );

            return new MessageResponse("Announcement marked as read");
        });
    }

    private String authenticatedUserId() {
        if (securityContext == null
                || securityContext.getUserPrincipal() == null) {
            throw new SecurityException("Authentication is required");
        }

        return securityContext.getUserPrincipal().getName();
    }

    private AnnouncementAudience parseAudience(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("audience is required");
        }

        try {
            return AnnouncementAudience.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Audience must be ALL, STUDENTS or STAFF"
            );
        }
    }

    private UUID parseId(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid announcement ID"
            );
        }
    }

    private void requireBody(Object body) {
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }
    }

    private Response respond(Supplier<Object> operation) {
        try {
            Object result = operation.get();

            if (result instanceof Response response) {
                return response;
            }

            return Response.ok(result).build();

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
                return error(
                        Response.Status.FORBIDDEN,
                        cause.getMessage()
                );
            }

            return error(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Announcement operation failed"
            );

        } catch (RuntimeException exception) {
            return error(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Announcement operation failed"
            );
        }
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new MessageResponse(message))
                .build();
    }
}
