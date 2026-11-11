package edu.nipa.scasp.domain;

import java.util.Objects;

public final class StudentManagementService {
    private final Repository<String, User> users;
    private final StudentRepository students;

    public StudentManagementService(Repository<String, User> users, StudentRepository students) {
        this.users = Objects.requireNonNull(users, "users");
        this.students = Objects.requireNonNull(students, "students");
    }

    public synchronized void create(User actor, Student student) {
        requireStaff(actor);
        Objects.requireNonNull(student, "student");
        User linkedUser = users.findById(student.userId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown linked user"));
        if (linkedUser.role() != UserRole.STUDENT || linkedUser.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Student profile requires an active student account");
        }
        if (students.findById(student.studentId()).isPresent()) {
            throw new IllegalArgumentException("Student already exists: " + student.studentId());
        }
        if (students.findAll().stream().anyMatch(existing ->
                existing.userId().equals(student.userId()))) {
            throw new IllegalArgumentException("A user may have only one student profile");
        }
        students.save(student.studentId(), student);
    }

    public synchronized void update(User actor, Student student) {
        requireStaff(actor);
        Objects.requireNonNull(student, "student");
        if (students.findById(student.studentId()).isEmpty()) {
            throw new IllegalArgumentException("Unknown student: " + student.studentId());
        }
        if (students.findAll().stream().anyMatch(existing ->
                !existing.studentId().equals(student.studentId())
                        && existing.userId().equals(student.userId()))) {
            throw new IllegalArgumentException("A user may have only one student profile");
        }
        User linkedUser = users.findById(student.userId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown linked user"));
        if (linkedUser.role() != UserRole.STUDENT || linkedUser.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Student profile requires an active student account");
        }
        students.save(student.studentId(), student);
    }

    public Student get(User actor, String studentId) {
        requireActiveUser(actor);
        Student student = students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown student: " + studentId));
        if (actor.role() == UserRole.STUDENT && !actor.userId().equals(student.userId())) {
            throw new SecurityException("Students may only view their own profile");
        }
        return student;
    }

    private void requireStaff(User actor) {
        requireActiveUser(actor);
        if (actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
    }

    private void requireActiveUser(User actor) {
        Objects.requireNonNull(actor, "actor");
        User current = users.findById(actor.userId())
                .orElseThrow(() -> new SecurityException("An active user is required"));
        if (!current.equals(actor) || current.status() != UserStatus.ACTIVE) {
            throw new SecurityException("An active user is required");
        }
    }
}
