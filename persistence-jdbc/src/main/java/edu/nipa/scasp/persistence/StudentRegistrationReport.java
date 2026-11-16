package edu.nipa.scasp.persistence;

import java.time.Instant;

public record StudentRegistrationReport(
        String studentId,
        String courseCode,
        String courseTitle,
        String registrationStatus,
        Instant registeredAt
) {
}
