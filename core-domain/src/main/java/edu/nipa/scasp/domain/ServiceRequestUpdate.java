package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ServiceRequestUpdate(
        UUID updateId,
        UUID requestId,
        String authorUserId,
        String note,
        Instant createdAt
) {
    public ServiceRequestUpdate {
        Objects.requireNonNull(updateId, "updateId");
        Objects.requireNonNull(requestId, "requestId");
        authorUserId = User.requireText(authorUserId, "authorUserId");
        note = User.requireText(note, "note");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
