package edu.nipa.scasp.concurrent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskResult(
        UUID taskId,
        TaskType taskType,
        String message,
        Instant completedAt
) {
    public TaskResult {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(taskType, "taskType");

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        Objects.requireNonNull(completedAt, "completedAt");
    }
}