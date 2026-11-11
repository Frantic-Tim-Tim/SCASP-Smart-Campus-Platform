package edu.nipa.scasp.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CourseManagementService {
    private final CourseRepository courses;
    private final RegistrationService registrations;

    public CourseManagementService(CourseRepository courses, RegistrationService registrations) {
        this.courses = Objects.requireNonNull(courses, "courses");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    public synchronized void create(User actor, Course course) {
        requireStaff(actor);
        Objects.requireNonNull(course, "course");
        if (courses.findById(course.courseCode()).isPresent()) {
            throw new IllegalArgumentException("Course already exists: " + course.courseCode());
        }
        registrations.addCourse(course);
        courses.save(course.courseCode(), course);
    }

    public synchronized void update(User actor, Course course) {
        requireStaff(actor);
        Objects.requireNonNull(course, "course");
        if (courses.findById(course.courseCode()).isEmpty()) {
            throw new IllegalArgumentException("Unknown course: " + course.courseCode());
        }
        registrations.updateCourse(course);
        courses.save(course.courseCode(), course);
    }

    public List<Course> search(String query) {
        return courses.search(query);
    }

    public List<Course> sorted(Comparator<Course> ordering) {
        return courses.findAll(Objects.requireNonNull(ordering, "ordering"));
    }

    private static void requireStaff(User actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor.status() != UserStatus.ACTIVE || actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
    }
}
