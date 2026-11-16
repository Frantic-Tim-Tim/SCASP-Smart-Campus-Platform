package edu.nipa.scasp.persistence;

public record CourseEnrollmentReport(
        String courseCode,
        String courseTitle,
        int capacity,
        String courseStatus,
        long enrolledCount,
        long waitlistCount,
        long availableSeats
) {
}
