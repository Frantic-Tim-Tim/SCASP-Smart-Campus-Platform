package edu.nipa.scasp.web;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Collectors;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private UserManagementService userManagementService;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication required."
            );
            return;
        }

        try {
            // Resolve the currently authenticated administrator.
            User administrator =
                    userManagementService.getUserById(principal.getName());

            // Retrieve all users. The service checks administrator access.
            List<User> users =
                    userManagementService.getAllUsers(administrator);

            // Calculate account statistics.
            Map<String, Long> statusCounts = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.status().name(),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            // Calculate the number of users in each role.
            Map<String, Long> roleCounts = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.role().name(),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            // Convert Java records into JavaBean-compatible view objects.
            UserView administratorView = new UserView(administrator);

            List<UserView> userViews = users.stream()
                    .map(UserView::new)
                    .toList();

            // Pass the data to the JSP.
            request.setAttribute("administrator", administratorView);
            request.setAttribute("users", userViews);
            request.setAttribute("userCount", users.size());

            request.setAttribute(
                    "activeCount",
                    statusCounts.getOrDefault("ACTIVE", 0L)
            );

            request.setAttribute(
                    "suspendedCount",
                    statusCounts.getOrDefault("SUSPENDED", 0L)
            );

            request.setAttribute("statusCounts", statusCounts);
            request.setAttribute("roleCounts", roleCounts);

            request.getRequestDispatcher(
                    "/WEB-INF/views/admin/dashboard.jsp"
            ).forward(request, response);

        } catch (NoSuchElementException | SecurityException exception) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "You are not authorised to access this dashboard."
            );

        } catch (Exception exception) {

            log("Unable to load the SCASP administration dashboard.", exception);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The dashboard could not be loaded."
            );
        }
    }

    /**
     * JavaBean-compatible representation of a domain User record.
     * Used exclusively by the JSP presentation layer.
     */
    public static final class UserView {

        private final String userId;
        private final String name;
        private final String role;
        private final String status;

        public UserView(User user) {
            this.userId = user.userId();
            this.name = user.name();
            this.role = user.role().name();
            this.status = user.status().name();
        }

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public String getStatus() {
            return status;
        }
    }
}