package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ServiceRequest(
        UUID requestId,
        String studentId,
        String title,
        String description,
        String category,
        RequestPriority priority,
        RequestStatus status,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt
) {
    public ServiceRequest {
        Objects.requireNonNull(requestId, "requestId");
        studentId = User.requireText(studentId, "studentId");
        title = User.requireText(title, "title");
        description = User.requireText(description, "description");
        category = User.requireText(category, "category");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(status, "status");

        if (assignedTo != null && assignedTo.isBlank()) {
            throw new IllegalArgumentException(
                    "assignedTo must be null or non-blank"
            );
        }

        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "updatedAt must not precede createdAt"
            );
        }
    }

    public ServiceRequest(
            UUID requestId,
            String studentId,
            String category,
            RequestPriority priority,
            RequestStatus status,
            String assignedTo,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                requestId,
                studentId,
                "General request",
                "No description provided.",
                category,
                priority,
                status,
                assignedTo,
                createdAt,
                updatedAt
        );
    }

    public ServiceRequest transitionTo(
            RequestStatus nextStatus,
            Instant at
    ) {
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
            throw new IllegalStateException(
                    "Invalid request status transition: "
                    + status + " -> " + nextStatus
            );
        }

        if (at.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "transition time must not precede updatedAt"
            );
        }

        return new ServiceRequest(
                requestId, studentId, title, description, category,
                priority, nextStatus, assignedTo, createdAt, at
        );
    }

    public ServiceRequest assignTo(String staffUserId, Instant at) {
        String assignee = User.requireText(staffUserId, "staffUserId");
        Objects.requireNonNull(at, "at");

        if (at.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "assignment time must not precede updatedAt"
            );
        }

        return new ServiceRequest(
                requestId, studentId, title, description, category,
                priority, status, assignee, createdAt, at
        );
    }

    public ServiceRequest changePriority(
            RequestPriority nextPriority,
            Instant at
    ) {
        Objects.requireNonNull(nextPriority, "nextPriority");
        Objects.requireNonNull(at, "at");

        if (at.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "priority change time must not precede updatedAt"
            );
        }

        return new ServiceRequest(
                requestId, studentId, title, description, category,
                nextPriority, status, assignedTo, createdAt, at
        );
    }
}
