package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(UUID eventId, String actor, String action, String entityType,
                         String entityId, Instant occurredAt) {
    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        actor = User.requireText(actor, "actor");
        action = User.requireText(action, "action");
        entityType = User.requireText(entityType, "entityType");
        entityId = User.requireText(entityId, "entityId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}