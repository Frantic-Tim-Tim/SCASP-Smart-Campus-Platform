package edu.nipa.scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

@WebServlet("/staff/dashboard")
@ServletSecurity(
    @HttpConstraint(rolesAllowed = {"STAFF", "ADMIN"})
)
public class StaffDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setAttribute("staffUserId", principal.getName());

        request.getRequestDispatcher(
                "/WEB-INF/views/staff/dashboard.jsp"
        ).forward(request, response);
    }
}
