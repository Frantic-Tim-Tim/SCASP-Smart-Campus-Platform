package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RegistrationService {
    private final Map<String, Course> courses = new HashMap<>();
    private final Map<RegistrationKey, Registration> registrations = new HashMap<>();
    private final Map<String, ArrayDeque<WaitlistEntry>> waitlists = new HashMap<>();
    private final Set<RegistrationKey> activeRegistrations = new HashSet<>();

    public synchronized void addCourse(Course course) {
        Objects.requireNonNull(course, "course");
        if (courses.putIfAbsent(course.courseCode(), course) != null) {
            throw new IllegalArgumentException("Course already exists: " + course.courseCode());
        }
    }

    public synchronized void updateCourse(Course course) {
        Objects.requireNonNull(course, "course");
        if (!courses.containsKey(course.courseCode())) {
            throw new IllegalArgumentException("Unknown course: " + course.courseCode());
        }
        if (enrolledCount(course.courseCode()) > course.capacity()) {
            throw new IllegalArgumentException("Capacity cannot be lower than current enrolment");
        }
        courses.put(course.courseCode(), course);
    }

    public synchronized RegistrationAttempt register(User actor, Student student,
                                                      String courseCode, Instant now) {
        authorize(actor, student);
        Objects.requireNonNull(now, "now");
        Course course = requireCourse(courseCode);
        if (course.status() != CourseStatus.OPEN) {
            throw new IllegalStateException("Course is not open: " + courseCode);
        }

        RegistrationKey key = new RegistrationKey(student.studentId(), courseCode);
        Registration existing = registrations.get(key);
        ArrayDeque<WaitlistEntry> queue = waitlists.computeIfAbsent(courseCode,
                ignored -> new ArrayDeque<>());
        if (activeRegistrations.contains(key)
                || queue.stream().anyMatch(entry -> entry.studentId().equals(student.studentId()))) {
            throw new IllegalStateException("Student already registered or waitlisted");
        }

        if (enrolledCount(courseCode) < course.capacity() && queue.isEmpty()) {
            Registration registration = existing == null
                    ? new Registration(UUID.randomUUID(), student.studentId(), courseCode,
                    RegistrationStatus.ENROLLED, now)
                    : existing.transitionTo(RegistrationStatus.ENROLLED);
            registrations.put(key, registration);
            activeRegistrations.add(key);
            return new RegistrationAttempt(registration, null);
        }

        WaitlistEntry entry = new WaitlistEntry(UUID.randomUUID(), student.studentId(),
                courseCode, queue.size() + 1, now);
        queue.addLast(entry);
        return new RegistrationAttempt(null, entry);
    }

    public synchronized Registration withdraw(User actor, Student student,
                                               String courseCode, Instant now) {
        authorize(actor, student);
        Objects.requireNonNull(now, "now");
        RegistrationKey key = new RegistrationKey(student.studentId(), courseCode);
        Registration current = registrations.get(key);
        if (current == null || current.status() != RegistrationStatus.ENROLLED) {
            throw new IllegalStateException("No active registration to withdraw");
        }

        Registration withdrawn = current.transitionTo(RegistrationStatus.WITHDRAWN);
        registrations.put(key, withdrawn);
        activeRegistrations.remove(key);
        promoteNext(courseCode, now);
        return withdrawn;
    }

    public synchronized List<Registration> registrations() {
        return registrations.values().stream().toList();
    }

    public synchronized List<WaitlistEntry> waitlist(String courseCode) {
        return List.copyOf(waitlists.getOrDefault(courseCode, new ArrayDeque<>()));
    }

    private void promoteNext(String courseCode, Instant now) {
        Course course = requireCourse(courseCode);
        ArrayDeque<WaitlistEntry> queue = waitlists.get(courseCode);
        if (course.status() != CourseStatus.OPEN || queue == null || queue.isEmpty()
                || enrolledCount(courseCode) >= course.capacity()) {
            return;
        }
        WaitlistEntry next = queue.removeFirst();
        RegistrationKey key = new RegistrationKey(next.studentId(), courseCode);
        Registration previous = registrations.get(key);
        Registration promoted = new Registration(UUID.randomUUID(), next.studentId(), courseCode,
                RegistrationStatus.ENROLLED, now);
        if (previous != null && previous.status() == RegistrationStatus.WITHDRAWN) {
            promoted = new Registration(previous.registrationId(), next.studentId(), courseCode,
                    RegistrationStatus.ENROLLED, previous.createdAt());
        }
        registrations.put(key, promoted);
        activeRegistrations.add(key);
        reindex(queue);
    }

    private static void reindex(ArrayDeque<WaitlistEntry> queue) {
        List<WaitlistEntry> entries = new ArrayList<>(queue);
        queue.clear();
        for (int index = 0; index < entries.size(); index++) {
            WaitlistEntry old = entries.get(index);
            queue.addLast(new WaitlistEntry(old.entryId(), old.studentId(), old.courseCode(),
                    index + 1, old.createdAt()));
        }
    }

    private long enrolledCount(String courseCode) {
        return registrations.values().stream()
                .filter(registration -> registration.courseCode().equals(courseCode)
                        && registration.status() == RegistrationStatus.ENROLLED)
                .count();
    }

    private Course requireCourse(String courseCode) {
        Course course = courses.get(User.requireText(courseCode, "courseCode"));
        if (course == null) {
            throw new IllegalArgumentException("Unknown course: " + courseCode);
        }
        return course;
    }

    private static void authorize(User actor, Student student) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(student, "student");
        if (actor.status() != UserStatus.ACTIVE || actor.role() != UserRole.STUDENT
                || !actor.userId().equals(student.userId())) {
            throw new SecurityException("Only the active student may manage their registration");
        }
    }

    private record RegistrationKey(String studentId, String courseCode) {
    }
}