package edu.nipa.scasp.persistence;

import java.time.Instant;

public record CourseWaitlistReport(
        String courseCode,
        String courseTitle,
        String studentId,
        int position,
        Instant waitlistedAt
) {
}
