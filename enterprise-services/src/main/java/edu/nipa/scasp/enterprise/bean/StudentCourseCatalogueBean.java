package edu.nipa.scasp.enterprise.bean;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.CourseRepository;
import edu.nipa.scasp.enterprise.service.StudentCourseCatalogueService;
import edu.nipa.scasp.enterprise.service.UserManagementService;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(StudentCourseCatalogueService.class)
public class StudentCourseCatalogueBean
        implements StudentCourseCatalogueService {

    @EJB
    private CourseRepository courseRepository;

    @EJB
    private UserManagementService userManagementService;

    public StudentCourseCatalogueBean() {
    }

    @Override
    public List<Course> getAvailableCourses(
            String authenticatedUserId,
            String search) {

        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");

        if (authenticatedUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user ID must not be blank"
            );
        }

        User student = userManagementService.getUserById(
                authenticatedUserId
        );

        if (student.role() != UserRole.STUDENT
                || student.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "An active student account is required"
            );
        }

        String query = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        return courseRepository.findAll().stream()
                .filter(course -> course.status() == CourseStatus.OPEN)
                .filter(course -> query.isEmpty()
                        || course.courseCode()
                                .toLowerCase(Locale.ROOT)
                                .contains(query)
                        || course.title()
                                .toLowerCase(Locale.ROOT)
                                .contains(query))
                .toList();
    }
}
