package edu.nipa.scasp.concurrent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

public record CampusTask(
        UUID taskId,
        TaskType taskType,
        String description,
        Instant submittedAt
) implements Callable<TaskResult> {

    public CampusTask {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(taskType, "taskType");

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "description must not be blank"
            );
        }

        Objects.requireNonNull(submittedAt, "submittedAt");
    }

    @Override
    public TaskResult call() throws Exception {
        // Simulate work performed by the SCASP system.
        return new TaskResult(
                taskId,
                taskType,
                "Successfully processed: " + description,
                Instant.now()
        );
    }
}