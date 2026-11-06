package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ServiceRequest(UUID requestId, String studentId, String category,
                             RequestPriority priority, RequestStatus status, String assignedTo,
                             Instant createdAt, Instant updatedAt) {
    public ServiceRequest {
        Objects.requireNonNull(requestId, "requestId");
        studentId = User.requireText(studentId, "studentId");
        category = User.requireText(category, "category");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(status, "status");
        if (assignedTo != null && assignedTo.isBlank()) {
            throw new IllegalArgumentException("assignedTo must be null or non-blank");
        }
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    public ServiceRequest transitionTo(RequestStatus nextStatus, Instant at) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        Objects.requireNonNull(at, "at");
        boolean valid = switch (status) {
            case SUBMITTED -> nextStatus == RequestStatus.IN_PROGRESS
                    || nextStatus == RequestStatus.CANCELLED;
            case IN_PROGRESS -> nextStatus == RequestStatus.RESOLVED
                    || nextStatus == RequestStatus.CANCELLED;
            case RESOLVED -> nextStatus == RequestStatus.CLOSED;
            case CLOSED, CANCELLED -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Invalid request status transition: "
                    + status + " -> " + nextStatus);
        }
        if (at.isBefore(updatedAt)) {
            throw new IllegalArgumentException("transition time must not precede updatedAt");
        }
        return new ServiceRequest(requestId, studentId, category, priority, nextStatus,
                assignedTo, createdAt, at);
    }
}