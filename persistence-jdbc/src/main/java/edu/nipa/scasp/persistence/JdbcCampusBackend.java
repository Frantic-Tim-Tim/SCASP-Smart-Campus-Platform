package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.CampusBackend;
import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.CourseCapacityReport;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.StudentRepository;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

public final class JdbcCampusBackend implements CampusBackend {
    private final JdbcAuthenticationService authentication;
    private final JdbcUserRepository users;
    private final JdbcCourseRepository courses;
    private final StudentRepository students;
    private final JdbcRegistrationService registrations;
    private final JdbcCourseManagementService courseManagement;
    private final JdbcReports reports;

    public JdbcCampusBackend(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        authentication = new JdbcAuthenticationService(dataSource);
        users = new JdbcUserRepository(dataSource);
        courses = new JdbcCourseRepository(dataSource);
        students = new JdbcStudentRepository(dataSource);
        registrations = new JdbcRegistrationService(dataSource);
        courseManagement = new JdbcCourseManagementService(dataSource);
        reports = new JdbcReports(dataSource);
    }

    @Override
    public User authenticate(String userId, char[] password) {
        return authentication.authenticate(userId, password);
    }

    @Override
    public List<Course> listCourses(String query) {
        return query == null || query.isBlank() ? courses.findAll() : courses.search(query);
    }

    @Override
    public Student getStudent(User actor, String studentId) {
        requireCurrentUser(actor);
        Student student = students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown student: " + studentId));
        if (actor.role() == UserRole.STUDENT && !actor.userId().equals(student.userId())) {
            throw new SecurityException("Students may only view their own profile");
        }
        return student;
    }

    @Override
    public RegistrationAttempt register(User actor, String studentId, String courseCode,
                                        Instant at) {
        return registrations.register(actor, studentId, courseCode, at);
    }

    @Override
    public Registration withdraw(User actor, String studentId, String courseCode, Instant at) {
        return registrations.withdraw(actor, studentId, courseCode, at);
    }

    @Override
    public void createOrUpdateCourse(User actor, Course course) {
        courseManagement.createOrUpdate(actor, course);
    }

    @Override
    public List<CourseCapacityReport> courseCapacityReport(User actor) {
        requireCurrentUser(actor);
        if (actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
        return reports.courseCapacity().stream()
                .map(item -> new CourseCapacityReport(item.courseCode(), item.title(),
                        item.capacity(), item.enrolled(), item.available(), item.waiting()))
                .toList();
    }

    private void requireCurrentUser(User actor) {
        Objects.requireNonNull(actor, "actor");
        User current = users.findById(actor.userId())
                .orElseThrow(() -> new SecurityException("An active user is required"));
        if (actor.status() != UserStatus.ACTIVE || current.status() != UserStatus.ACTIVE
                || actor.role() != current.role()) {
            throw new SecurityException("An active user is required");
        }
    }
}
