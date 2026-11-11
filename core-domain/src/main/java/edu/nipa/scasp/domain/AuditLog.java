package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AuditLog {
    private final ConcurrentLinkedQueue<AuditEvent> events = new ConcurrentLinkedQueue<>();

    public AuditEvent record(String actor, String action, String entityType, String entityId,
                             Instant at) {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), actor, action, entityType,
                entityId, Objects.requireNonNull(at, "at"));
        events.add(event);
        return event;
    }

    public List<AuditEvent> events() {
        return events.stream()
                .sorted(Comparator.comparing(AuditEvent::occurredAt))
                .toList();
    }
}
