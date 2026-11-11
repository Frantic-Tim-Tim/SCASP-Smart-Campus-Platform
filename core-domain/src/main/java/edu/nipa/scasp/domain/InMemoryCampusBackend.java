package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCampusBackend implements CampusBackend {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AuthenticationService authentication = new AuthenticationService();
    private final StudentRepository students = new InMemoryStudentRepository();
    private final CourseRepository courses = new InMemoryCourseRepository();
    private final RegistrationService registrations = new RegistrationService();
    private final AuditLog auditLog = new AuditLog();
    private final CourseManagementService courseManagement =
            new CourseManagementService(courses, registrations);

    public synchronized void addUser(User user) {
        Objects.requireNonNull(user, "user");
        if (users.putIfAbsent(user.userId(), user) != null) {
            throw new IllegalArgumentException("User already exists: " + user.userId());
        }
        auditLog.record(user.userId(), "USER_CREATED", "User", user.userId(), Instant.now());
    }

    public synchronized void addUser(User user, char[] password) {
        Objects.requireNonNull(user, "user");
        if (users.containsKey(user.userId())) {
            throw new IllegalArgumentException("User already exists: " + user.userId());
        }
        authentication.register(user, password);
        users.put(user.userId(), user);
        auditLog.record(user.userId(), "USER_CREATED", "User", user.userId(), Instant.now());
    }

    @Override
    public User authenticate(String userId, char[] password) {
        User user = authentication.authenticate(userId, password);
        if (users.get(user.userId()) != user) {
            throw new SecurityException("Invalid credentials");
        }
        return user;
    }

    public synchronized void addStudent(Student student) {
        Objects.requireNonNull(student, "student");
        User user = users.get(student.userId());
        if (user == null || user.role() != UserRole.STUDENT || user.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Student must reference an active student account");
        }
        if (students.findById(student.studentId()).isPresent()) {
            throw new IllegalArgumentException("Student already exists: " + student.studentId());
        }
        if (students.findAll().stream().anyMatch(existing ->
                existing.userId().equals(student.userId()))) {
            throw new IllegalArgumentException("A user may have only one student profile");
        }
        students.save(student.studentId(), student);
        auditLog.record(student.userId(), "STUDENT_CREATED", "Student", student.studentId(),
                Instant.now());
    }

    public void addCourse(User actor, Course course) {
        requireActiveStaff(actor);
        courseManagement.create(actor, course);
        auditLog.record(actor.userId(), "COURSE_CREATED", "Course", course.courseCode(),
                Instant.now());
    }

    public void createOrUpdateCourse(User actor, Course course) {
        requireActiveStaff(actor);
        boolean existed = courses.findById(course.courseCode()).isPresent();
        if (existed) {
            courseManagement.update(actor, course);
        } else {
            courseManagement.create(actor, course);
        }
        auditLog.record(actor.userId(), existed ? "COURSE_UPDATED" : "COURSE_CREATED",
                "Course", course.courseCode(), Instant.now());
    }

    @Override
    public List<CourseCapacityReport> courseCapacityReport(User actor) {
        requireActiveStaff(actor);
        List<Registration> currentRegistrations = registrations.registrations();
        Map<String, Long> enrolledByCourse = currentRegistrations.stream()
                .filter(item -> item.status() == RegistrationStatus.ENROLLED)
                .collect(Collectors.groupingBy(Registration::courseCode, Collectors.counting()));
        return courses.findAll().stream().map(course -> {
            long enrolled = enrolledByCourse.getOrDefault(course.courseCode(), 0L);
            int waiting = registrations.waitlist(course.courseCode()).size();
            return new CourseCapacityReport(course.courseCode(), course.title(), course.capacity(),
                    Math.toIntExact(enrolled), course.capacity() - Math.toIntExact(enrolled),
                    waiting);
        }).toList();
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
        Student student = requireStudent(actor, studentId);
        RegistrationAttempt attempt = registrations.register(actor, student, courseCode, at);
        auditLog.record(actor.userId(), attempt.isWaitlisted() ? "WAITLISTED" : "REGISTERED",
                attempt.isWaitlisted() ? "WaitlistEntry" : "Registration",
                studentId + ":" + courseCode, at);
        return attempt;
    }

    @Override
    public Registration withdraw(User actor, String studentId, String courseCode, Instant at) {
        Student student = requireStudent(actor, studentId);
        Registration withdrawn = registrations.withdraw(actor, student, courseCode, at);
        auditLog.record(actor.userId(), "WITHDRAWN", "Registration",
                studentId + ":" + courseCode, at);
        return withdrawn;
    }

    public List<Registration> registrations() {
        return registrations.registrations();
    }

    public List<WaitlistEntry> waitlist(String courseCode) {
        return registrations.waitlist(courseCode);
    }

    public List<AuditEvent> auditEvents() {
        return auditLog.events();
    }

    private User requireActiveStaff(User actor) {
        User current = requireCurrentUser(actor);
        if (current.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
        return current;
    }

    private Student requireStudent(User actor, String studentId) {
        requireCurrentUser(actor);
        return students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown student: " + studentId));
    }

    private User requireCurrentUser(User actor) {
        Objects.requireNonNull(actor, "actor");
        User current = users.get(actor.userId());
        if (current == null || !current.equals(actor) || current.status() != UserStatus.ACTIVE) {
            throw new SecurityException("An active user is required");
        }
        return current;
    }
}
