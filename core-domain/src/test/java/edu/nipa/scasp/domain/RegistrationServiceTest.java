package edu.nipa.scasp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistrationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private RegistrationService registrations;

    @BeforeEach
    void setUp() {
        registrations = new RegistrationService();
        registrations.addCourse(new Course("CS101", "Computing", 1, CourseStatus.OPEN));
    }

    @Test
    void capacityIsNeverExceededAndWaitlistIsOrdered() {
        Student first = student("student-1", "user-1");
        Student second = student("student-2", "user-2");
        Student third = student("student-3", "user-3");

        assertFalse(registrations.register(user(first), first, "CS101", NOW).isWaitlisted());
        RegistrationAttempt secondAttempt = registrations.register(user(second), second,
                "CS101", NOW);
        RegistrationAttempt thirdAttempt = registrations.register(user(third), third,
                "CS101", NOW);

        assertTrue(secondAttempt.isWaitlisted());
        assertTrue(thirdAttempt.isWaitlisted());
        assertEquals(1, registrations.registrations().stream()
                .filter(registration -> registration.status() == RegistrationStatus.ENROLLED)
                .count());
        assertEquals(List.of(1, 2), registrations.waitlist("CS101").stream()
                .map(WaitlistEntry::position).toList());
    }

    @Test
    void duplicateActiveRegistrationIsRejected() {
        Student student = student("student-1", "user-1");
        registrations.register(user(student), student, "CS101", NOW);

        assertThrows(IllegalStateException.class,
                () -> registrations.register(user(student), student, "CS101", NOW));
    }

    @Test
    void withdrawalPromotesTheFirstWaitlistedStudent() {
        Student first = student("student-1", "user-1");
        Student second = student("student-2", "user-2");
        registrations.register(user(first), first, "CS101", NOW);
        registrations.register(user(second), second, "CS101", NOW);

        registrations.withdraw(user(first), first, "CS101", NOW.plusSeconds(60));

        assertEquals(RegistrationStatus.WITHDRAWN, registrations.registrations().stream()
                .filter(registration -> registration.studentId().equals(first.studentId()))
                .findFirst().orElseThrow().status());
        assertEquals(RegistrationStatus.ENROLLED, registrations.registrations().stream()
                .filter(registration -> registration.studentId().equals(second.studentId()))
                .findFirst().orElseThrow().status());
        assertTrue(registrations.waitlist("CS101").isEmpty());
    }

    @Test
    void anotherUserCannotRegisterOnAStudentsBehalf() {
        Student student = student("student-1", "user-1");
        User actor = new User("staff-1", "Staff", UserRole.STAFF, UserStatus.ACTIVE);

        assertThrows(SecurityException.class,
                () -> registrations.register(actor, student, "CS101", NOW));
    }

    @Test
    void invalidRegistrationStatusTransitionIsRejected() {
        Registration registration = new Registration(UUID.randomUUID(), "student-1", "CS101",
                RegistrationStatus.ENROLLED, NOW);

        assertThrows(IllegalStateException.class,
                () -> registration.transitionTo(RegistrationStatus.ENROLLED));
        assertEquals(RegistrationStatus.WITHDRAWN,
                registration.transitionTo(RegistrationStatus.WITHDRAWN).status());
    }

    @Test
    void registrationIdentifierRemainsStableWhenStudentReRegisters() {
        Student student = student("student-1", "user-1");
        Registration original = registrations.register(user(student), student, "CS101", NOW)
                .registration();
        registrations.withdraw(user(student), student, "CS101", NOW.plusSeconds(60));

        Registration renewed = registrations.register(user(student), student, "CS101",
                NOW.plusSeconds(120)).registration();

        assertEquals(original.registrationId(), renewed.registrationId());
        assertEquals(original.createdAt(), renewed.createdAt());
    }

    @Test
    void serviceRequestAllowsOnlyValidForwardTransitions() {
        ServiceRequest request = new ServiceRequest(UUID.randomUUID(), "student-1", "Academic",
                RequestPriority.NORMAL, RequestStatus.SUBMITTED, null, NOW, NOW);

        ServiceRequest inProgress = request.transitionTo(RequestStatus.IN_PROGRESS,
                NOW.plusSeconds(60));

        assertEquals(RequestStatus.IN_PROGRESS, inProgress.status());
        assertThrows(IllegalStateException.class,
                () -> inProgress.transitionTo(RequestStatus.SUBMITTED, NOW.plusSeconds(120)));
    }

    private static Student student(String studentId, String userId) {
        return new Student(studentId, userId, "Computer Science", 1);
    }

    private static User user(Student student) {
        return new User(student.userId(), student.userId(), UserRole.STUDENT, UserStatus.ACTIVE);
    }
}