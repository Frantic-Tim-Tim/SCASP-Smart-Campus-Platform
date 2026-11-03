package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WaitlistEntry(UUID entryId, String studentId, String courseCode,
                            int position, Instant createdAt) {
    public WaitlistEntry {
        Objects.requireNonNull(entryId, "entryId");
        studentId = User.requireText(studentId, "studentId");
        courseCode = User.requireText(courseCode, "courseCode");
        if (position < 1) {
            throw new IllegalArgumentException("position must be positive");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }
}