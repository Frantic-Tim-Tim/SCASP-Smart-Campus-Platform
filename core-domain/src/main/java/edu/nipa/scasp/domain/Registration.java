package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Registration(UUID registrationId, String studentId, String courseCode,
                           RegistrationStatus status, Instant createdAt) {
    public Registration {
        Objects.requireNonNull(registrationId, "registrationId");
        studentId = User.requireText(studentId, "studentId");
        courseCode = User.requireText(courseCode, "courseCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public Registration transitionTo(RegistrationStatus nextStatus) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        if (status == nextStatus || (status == RegistrationStatus.ENROLLED
                && nextStatus != RegistrationStatus.WITHDRAWN)
                || (status == RegistrationStatus.WITHDRAWN
                && nextStatus != RegistrationStatus.ENROLLED)) {
            throw new IllegalStateException("Invalid registration status transition: "
                    + status + " -> " + nextStatus);
        }
        return new Registration(registrationId, studentId, courseCode, nextStatus, createdAt);
    }
}