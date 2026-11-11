package edu.nipa.scasp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainServicesTest {
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @Test
    void courseRepositorySearchesAndSortsWithoutExposingMutableState() {
        InMemoryCourseRepository courses = new InMemoryCourseRepository();
        courses.save("CS202", new Course("CS202", "Data Structures", 20, CourseStatus.OPEN));
        courses.save("CS101", new Course("CS101", "Programming", 30, CourseStatus.OPEN));

        assertEquals("CS101", courses.findAll().getFirst().courseCode());
        assertEquals(2, courses.findAll(Comparator.comparing(Course::title)).size());
        assertEquals(1, courses.search("structure").size());
        assertThrows(UnsupportedOperationException.class, () -> courses.findAll().clear());
    }

    @Test
    void courseValueEqualityAndNaturalOrderingAreStable() {
        Course first = new Course("CS101", "Programming", 30, CourseStatus.OPEN);
        Course same = new Course("CS101", "Programming", 30, CourseStatus.OPEN);
        Course second = new Course("CS202", "Data Structures", 20, CourseStatus.OPEN);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void courseManagementRequiresStaffAndPreservesCurrentCapacity() {
        InMemoryCourseRepository courses = new InMemoryCourseRepository();
        RegistrationService registrations = new RegistrationService();
        CourseManagementService management = new CourseManagementService(courses, registrations);
        User staff = new User("staff-1", "Academic Staff", UserRole.STAFF, UserStatus.ACTIVE);
        Course original = new Course("CS101", "Computing", 2, CourseStatus.OPEN);
        courses.save(original.courseCode(), original);
        registrations.addCourse(original);
        Student first = new Student("student-1", "user-1", "Computing", 1);
        Student second = new Student("student-2", "user-2", "Computing", 1);
        registrations.register(new User("user-1", "Student One", UserRole.STUDENT,
                UserStatus.ACTIVE), first, "CS101", NOW);
        registrations.register(new User("user-2", "Student Two", UserRole.STUDENT,
                UserStatus.ACTIVE), second, "CS101", NOW);
        User user = new User("user-3", "Student Three", UserRole.STUDENT, UserStatus.ACTIVE);

        assertThrows(SecurityException.class, () -> management.create(user,
                new Course("CS102", "Networks", 1, CourseStatus.OPEN)));
        assertThrows(IllegalArgumentException.class, () -> management.update(staff,
                new Course("CS101", "Computing", 1, CourseStatus.OPEN)));
        assertEquals(2, courses.findById("CS101").orElseThrow().capacity());
    }

    @Test
    void authenticationUsesHashedCredentialsAndRejectsBadLogin() {
        AuthenticationService authentication = new AuthenticationService();
        User user = new User("user-1", "Demo User", UserRole.STUDENT, UserStatus.ACTIVE);
        char[] password = "correct horse battery".toCharArray();
        authentication.register(user, password);

        assertEquals(user, authentication.authenticate("user-1",
                "correct horse battery".toCharArray()));
        assertThrows(SecurityException.class, () -> authentication.authenticate("user-1",
                "incorrect password".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> authentication.register(user, "short".toCharArray()));
    }

    @Test
    void studentManagementChecksLinkedAccountsAndSelfServiceAccess() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryStudentRepository students = new InMemoryStudentRepository();
        StudentManagementService management = new StudentManagementService(users, students);
        User staff = new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE);
        User studentUser = new User("user-1", "Student", UserRole.STUDENT, UserStatus.ACTIVE);
        users.save(staff.userId(), staff);
        users.save(studentUser.userId(), studentUser);
        Student profile = new Student("student-1", studentUser.userId(), "Computing", 1);
        management.create(staff, profile);

        assertEquals(profile, management.get(studentUser, profile.studentId()));
        assertThrows(SecurityException.class, () -> management.get(
                new User("user-2", "Other Student", UserRole.STUDENT, UserStatus.ACTIVE),
                profile.studentId()));
        assertThrows(IllegalArgumentException.class, () -> management.create(staff, profile));
    }

    @Test
    void requestsArePrioritizedAndOnlyStaffCanAssign() {
        ServiceRequestManagementService requests = new ServiceRequestManagementService();
        Student student = new Student("student-1", "user-1", "Computing", 1);
        User owner = new User("user-1", "Student", UserRole.STUDENT, UserStatus.ACTIVE);
        User staff = new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE);
        ServiceRequest request = requests.submit(owner, student, "Transcript", NOW);

        assertThrows(SecurityException.class, () -> requests.update(owner, request.requestId(),
                null, RequestPriority.URGENT, RequestStatus.IN_PROGRESS, NOW.plusSeconds(5)));
        ServiceRequest updated = requests.update(staff, request.requestId(), staff,
                RequestPriority.URGENT, RequestStatus.IN_PROGRESS, NOW.plusSeconds(5));

        assertEquals(updated, requests.prioritizedWorkload().getFirst());
        assertEquals(staff.userId(), updated.assignedTo());
        assertEquals(2, requests.auditEvents().size());
    }

    @Test
    void announcementsAreFilteredByAudienceAndAuditIsAppendOnly() {
        AnnouncementManagementService announcements = new AnnouncementManagementService();
        User staff = new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE);
        User student = new User("user-1", "Student", UserRole.STUDENT, UserStatus.ACTIVE);
        announcements.publish(staff, new Announcement(UUID.randomUUID(), "For staff",
                "Internal notice", AnnouncementAudience.STAFF, NOW));
        announcements.publish(staff, new Announcement(UUID.randomUUID(), "For all",
                "Public notice", AnnouncementAudience.ALL, NOW.plusSeconds(1)));
        assertEquals(1, announcements.forAudience(student).size());
        assertEquals(2, announcements.auditEvents().size());

        AuditLog audit = new AuditLog();
        audit.record(staff.userId(), "COURSE_UPDATED", "Course", "CS101", NOW);
        assertEquals(1, audit.events().size());
        assertTrue(audit.events().getFirst().occurredAt().equals(NOW));
    }

    @Test
    void backendAuditsOperationsAndProtectsCapacityReports() {
        InMemoryCampusBackend backend = new InMemoryCampusBackend();
        User staff = new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE);
        User student = new User("user-1", "Student", UserRole.STUDENT, UserStatus.ACTIVE);
        backend.addUser(staff);
        backend.addUser(student);
        backend.addStudent(new Student("student-1", "user-1", "Computing", 1));
        backend.addCourse(staff, new Course("CS101", "Computing", 1, CourseStatus.OPEN));
        backend.register(student, "student-1", "CS101", NOW);

        assertEquals(1, backend.courseCapacityReport(staff).getFirst().enrolled());
        assertThrows(SecurityException.class, () -> backend.courseCapacityReport(student));
        assertEquals(5, backend.auditEvents().size());
    }
}
