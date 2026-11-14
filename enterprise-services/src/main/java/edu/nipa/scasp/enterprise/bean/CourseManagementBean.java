package edu.nipa.scasp.enterprise.bean;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import edu.nipa.scasp.enterprise.service.DuplicateCourseException;
import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.CourseRepository;
import edu.nipa.scasp.enterprise.service.CourseManagementService;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(CourseManagementService.class)
public class CourseManagementBean implements CourseManagementService {

    @EJB
    private CourseRepository courseRepository;

    public CourseManagementBean() {
    }

    CourseManagementBean(CourseRepository courseRepository) {
        this.courseRepository = Objects.requireNonNull(courseRepository);
    }

    @Override
    public Course createCourse(User actor, Course course) {
        requireAdministrator(actor);
        Objects.requireNonNull(course, "course");

        if (courseRepository.existsByCode(course.courseCode())) {
            throw new DuplicateCourseException(
                "Course already exists: " + course.courseCode()
                );
        }

        return courseRepository.save(course);
    }

    @Override
    public Course getCourse(User actor, String courseCode) {
        requireAdministrator(actor);
        requireText(courseCode, "courseCode");

        return courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "Course not found: " + courseCode
                ));
    }

    @Override
    public List<Course> getCourses(
            User actor,
            String search,
            String sortBy,
            String direction
    ) {
        requireAdministrator(actor);

        String query = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        Comparator<Course> comparator = comparatorFor(sortBy);

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        } else if (direction != null
                && !direction.isBlank()
                && !"asc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException(
                    "Sort direction must be 'asc' or 'desc'"
            );
        }

        return courseRepository.findAll().stream()
                .filter(course -> query.isEmpty()
                        || course.courseCode().toLowerCase(Locale.ROOT)
                                .contains(query)
                        || course.title().toLowerCase(Locale.ROOT)
                                .contains(query))
                .sorted(comparator)
                .toList();
    }

    @Override
    public Course updateCourse(
            User actor,
            String courseCode,
            Course course
    ) {
        requireAdministrator(actor);
        requireText(courseCode, "courseCode");
        Objects.requireNonNull(course, "course");

        if (!courseCode.equals(course.courseCode())) {
            throw new IllegalArgumentException(
                    "The course code in the URL must match the course code in the request"
            );
        }

        if (!courseRepository.existsByCode(courseCode)) {
            throw new NoSuchElementException(
                    "Course not found: " + courseCode
            );
        }

        return courseRepository.save(course);
    }

    @Override
    public boolean deleteCourse(User actor, String courseCode) {
        requireAdministrator(actor);
        requireText(courseCode, "courseCode");

        if (!courseRepository.existsByCode(courseCode)) {
            throw new NoSuchElementException(
                    "Course not found: " + courseCode
            );
        }

        return courseRepository.deleteByCode(courseCode);
    }

    private Comparator<Course> comparatorFor(String sortBy) {
        String field = sortBy == null || sortBy.isBlank()
                ? "code"
                : sortBy.trim().toLowerCase(Locale.ROOT);

        return switch (field) {
            case "code" -> Comparator.comparing(Course::courseCode);
            case "title" -> Comparator.comparing(
                    Course::title,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "capacity" -> Comparator.comparingInt(Course::capacity);
            case "status" -> Comparator.comparing(
                    course -> course.status().name()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported sort field. Use code, title, capacity or status"
            );
        };
    }

    private void requireAdministrator(User actor) {
        Objects.requireNonNull(actor, "actor");

        if (actor.role() != UserRole.ADMIN
                || actor.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "An active administrator account is required"
            );
        }
    }

    private void requireText(String value, String field) {
        Objects.requireNonNull(value, field);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
    }
}
