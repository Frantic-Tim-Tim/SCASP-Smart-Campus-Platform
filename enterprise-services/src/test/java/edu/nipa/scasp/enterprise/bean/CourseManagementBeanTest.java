package edu.nipa.scasp.enterprise.bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseStatus;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.CourseRepository;
import edu.nipa.scasp.enterprise.service.DuplicateCourseException;

class CourseManagementBeanTest {

    private FakeCourseRepository repository;
    private CourseManagementBean service;

    private final User admin = new User(
            "ADM001",
            "System Administrator",
            UserRole.ADMIN,
            UserStatus.ACTIVE
    );

    private final User student = new User(
            "STU001",
            "John Student",
            UserRole.STUDENT,
            UserStatus.ACTIVE
    );

    private final Course java = new Course(
            "BSC3100",
            "Advanced Java Programming",
            60,
            CourseStatus.OPEN
    );

    private final Course database = new Course(
            "BSC2200",
            "Database Systems",
            80,
            CourseStatus.OPEN
    );

    private final Course networking = new Course(
            "BSC2300",
            "Computer Networks",
            40,
            CourseStatus.CLOSED
    );

    @BeforeEach
    void setUp() {
        repository = new FakeCourseRepository();
        service = new CourseManagementBean(repository);
    }

    @Test
    void administratorCanCreateCourse() {
        Course result = service.createCourse(admin, java);

        assertEquals(java, result);
        assertTrue(repository.existsByCode("BSC3100"));
    }

    @Test
    void duplicateCourseCannotBeCreated() {
        service.createCourse(admin, java);

        assertThrows(
                DuplicateCourseException.class,
                () -> service.createCourse(admin, java)
        );
    }

    @Test
    void administratorCanRetrieveCourseByCode() {
        repository.save(java);

        Course result = service.getCourse(admin, "BSC3100");

        assertEquals(java, result);
    }

    @Test
    void retrievingMissingCourseThrowsException() {
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> service.getCourse(admin, "UNKNOWN")
        );
    }

    @Test
    void administratorCanRetrieveAllCourses() {
        repository.save(java);
        repository.save(database);

        List<Course> result = service.getCourses(admin, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void coursesCanBeSearchedByCourseCode() {
        repository.save(java);
        repository.save(database);

        List<Course> result = service.getCourses(
                admin, "BSC3100", null, null
        );

        assertEquals(1, result.size());
        assertEquals("BSC3100", result.get(0).courseCode());
    }

    @Test
    void coursesCanBeSearchedByTitleWithoutCaseSensitivity() {
        repository.save(java);
        repository.save(database);

        List<Course> result = service.getCourses(
                admin, "DATABASE", null, null
        );

        assertEquals(1, result.size());
        assertEquals("BSC2200", result.get(0).courseCode());
    }

    @Test
    void coursesCanBeSortedByTitleAscending() {
        repository.save(java);
        repository.save(database);
        repository.save(networking);

        List<Course> result = service.getCourses(
                admin, null, "title", "asc"
        );

        assertEquals(
                List.of("Advanced Java Programming", "Computer Networks",
                        "Database Systems"),
                result.stream().map(Course::title).toList()
        );
    }

    @Test
    void coursesCanBeSortedByCapacityDescending() {
        repository.save(java);
        repository.save(database);
        repository.save(networking);

        List<Course> result = service.getCourses(
                admin, null, "capacity", "desc"
        );

        assertEquals(
                List.of(80, 60, 40),
                result.stream().map(Course::capacity).toList()
        );
    }

    @Test
    void unsupportedSortFieldIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCourses(admin, null, "lecturer", "asc")
        );
    }

    @Test
    void unsupportedSortDirectionIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCourses(admin, null, "code", "sideways")
        );
    }

    @Test
    void administratorCanUpdateCourse() {
        repository.save(java);

        Course updatedCourse = new Course(
                "BSC3100",
                "Advanced Java",
                80,
                CourseStatus.CLOSED
        );

        Course result = service.updateCourse(
                admin, "BSC3100", updatedCourse
        );

        assertEquals(updatedCourse, result);
        assertEquals(
                updatedCourse,
                repository.findByCode("BSC3100").orElseThrow()
        );
    }

    @Test
    void updateRejectsMismatchedCourseCode() {
        repository.save(java);

        Course mismatchedCourse = new Course(
                "BSC2200",
                "Database Systems",
                80,
                CourseStatus.OPEN
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateCourse(
                        admin, "BSC3100", mismatchedCourse
                )
        );
    }

    @Test
        void updatingMissingCourseThrowsException() {
                Course missingCourse = new Course(
                "UNKNOWN",
                "Unknown Course",
                50,
                CourseStatus.OPEN
                );

                assertThrows(
            java.util.NoSuchElementException.class,
            () -> service.updateCourse(admin, "UNKNOWN", missingCourse)
                );
        }

    @Test
    void administratorCanDeleteCourse() {
        repository.save(java);

        boolean deleted = service.deleteCourse(admin, "BSC3100");

        assertTrue(deleted);
        assertFalse(repository.existsByCode("BSC3100"));
    }

    @Test
    void deletingMissingCourseThrowsException() {
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> service.deleteCourse(admin, "UNKNOWN")
        );
    }

    @Test
    void nonAdministratorCannotCreateCourse() {
        assertThrows(
                SecurityException.class,
                () -> service.createCourse(student, java)
        );
    }

    @Test
    void nonAdministratorCannotRetrieveCourses() {
        assertThrows(
                SecurityException.class,
                () -> service.getCourses(student, null, null, null)
        );
    }

    @Test
    void suspendedAdministratorCannotManageCourses() {
        User suspendedAdmin = new User(
                "ADM002",
                "Suspended Administrator",
                UserRole.ADMIN,
                UserStatus.SUSPENDED
        );

        assertThrows(
                SecurityException.class,
                () -> service.createCourse(suspendedAdmin, java)
        );
    }

    private static class FakeCourseRepository implements CourseRepository {

        private final Map<String, Course> courses = new LinkedHashMap<>();

        @Override
        public Optional<Course> findByCode(String courseCode) {
            return Optional.ofNullable(courses.get(courseCode));
        }

        @Override
        public List<Course> findAll() {
            return new ArrayList<>(courses.values());
        }

        @Override
        public boolean existsByCode(String courseCode) {
            return courses.containsKey(courseCode);
        }

        @Override
        public Course save(Course course) {
            courses.put(course.courseCode(), course);
            return course;
        }

        @Override
        public boolean deleteByCode(String courseCode) {
            return courses.remove(courseCode) != null;
        }
    }
}
