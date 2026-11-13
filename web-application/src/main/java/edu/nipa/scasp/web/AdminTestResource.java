package edu.nipa.scasp.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.annotation.security.RolesAllowed;

@Path("admin/test")
@RolesAllowed("ADMIN")
public class AdminTestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String testAdminAccess() {
        return "{\"message\":\"SCASP authentication successful\","
                + "\"role\":\"ADMIN\"}";
    }
}