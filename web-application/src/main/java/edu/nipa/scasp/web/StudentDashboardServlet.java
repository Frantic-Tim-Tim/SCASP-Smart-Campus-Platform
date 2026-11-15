package edu.nipa.scasp.web;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.WaitlistEntry;
import edu.nipa.scasp.enterprise.repository.StudentProfileRepository;
import edu.nipa.scasp.enterprise.service.CourseRegistrationService;
import edu.nipa.scasp.enterprise.service.StudentCourseCatalogueService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/student/dashboard")
public class StudentDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private UserManagementService userManagementService;

    @EJB
    private StudentProfileRepository studentProfileRepository;

    @EJB
    private StudentCourseCatalogueService catalogueService;

    @EJB
    private CourseRegistrationService registrationService;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication is required."
            );
            return;
        }

        String userId = principal.getName();

        try {
            User user = userManagementService.getUserById(userId);

            Student student = studentProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Student profile was not found."
                    ));

            List<CourseView> courses =
                    catalogueService.getAvailableCourses(userId, null)
                            .stream()
                            .map(CourseView::new)
                            .toList();

            List<RegistrationView> registrations =
                    registrationService.getMyRegistrations(userId)
                            .stream()
                            .map(RegistrationView::new)
                            .toList();

            List<WaitlistView> waitlistEntries =
                    registrationService.getMyWaitlistEntries(userId)
                            .stream()
                            .map(WaitlistView::new)
                            .toList();

            request.setAttribute("studentName", user.name());
            request.setAttribute("studentId", student.studentId());
            request.setAttribute("programme", student.programme());
            request.setAttribute("yearOfStudy", student.yearOfStudy());

            request.setAttribute("courses", courses);
            request.setAttribute("registrations", registrations);
            request.setAttribute("waitlistEntries", waitlistEntries);

            request.setAttribute("courseCount", courses.size());
            request.setAttribute("registrationCount", registrations.size());
            request.setAttribute("waitlistCount", waitlistEntries.size());

            request.getRequestDispatcher(
                    "/WEB-INF/views/student/dashboard.jsp"
            ).forward(request, response);

        } catch (SecurityException exception) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "You are not authorised to access the student dashboard."
            );

        } catch (NoSuchElementException exception) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    exception.getMessage()
            );

        } catch (Exception exception) {
            log("Unable to load the student dashboard for user " + userId,
                    exception);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The student dashboard could not be loaded."
            );
        }
    }

    public static final class CourseView {
        private final String courseCode;
        private final String title;
        private final int capacity;
        private final String status;

        public CourseView(Course course) {
            this.courseCode = course.courseCode();
            this.title = course.title();
            this.capacity = course.capacity();
            this.status = course.status().name();
        }

        public String getCourseCode() { return courseCode; }
        public String getTitle() { return title; }
        public int getCapacity() { return capacity; }
        public String getStatus() { return status; }
    }

    public static final class RegistrationView {
        private final String registrationId;
        private final String courseCode;
        private final String status;
        private final String createdAt;

        public RegistrationView(Registration registration) {
            this.registrationId = registration.registrationId().toString();
            this.courseCode = registration.courseCode();
            this.status = registration.status().name();
            this.createdAt = registration.createdAt().toString();
        }

        public String getRegistrationId() { return registrationId; }
        public String getCourseCode() { return courseCode; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
    }

    public static final class WaitlistView {
        private final String entryId;
        private final String courseCode;
        private final int position;
        private final String createdAt;

        public WaitlistView(WaitlistEntry entry) {
            this.entryId = entry.entryId().toString();
            this.courseCode = entry.courseCode();
            this.position = entry.position();
            this.createdAt = entry.createdAt().toString();
        }

        public String getEntryId() { return entryId; }
        public String getCourseCode() { return courseCode; }
        public int getPosition() { return position; }
        public String getCreatedAt() { return createdAt; }
    }
}
