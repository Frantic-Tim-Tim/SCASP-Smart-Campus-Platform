package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;

public final class ServiceRequestManagementService {
    private static final Comparator<ServiceRequest> PRIORITY_ORDER =
            Comparator.comparing(ServiceRequest::priority).reversed()
                    .thenComparing(ServiceRequest::createdAt)
                    .thenComparing(ServiceRequest::requestId);

    private final Map<UUID, ServiceRequest> requests = new HashMap<>();
    private final PriorityQueue<ServiceRequest> workload = new PriorityQueue<>(PRIORITY_ORDER);
    private final AuditLog auditLog;

    public ServiceRequestManagementService() {
        this(new AuditLog());
    }

    public ServiceRequestManagementService(AuditLog auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    public synchronized ServiceRequest submit(User actor, Student student, String category,
                                              Instant now) {
        requireSelf(actor, student);
        Objects.requireNonNull(now, "now");
        ServiceRequest request = new ServiceRequest(UUID.randomUUID(), student.studentId(),
                category, RequestPriority.NORMAL, RequestStatus.SUBMITTED, null, now, now);
        save(request);
        auditLog.record(actor.userId(), "SERVICE_REQUEST_SUBMITTED", "ServiceRequest",
                request.requestId().toString(), now);
        return request;
    }

    public synchronized ServiceRequest update(User actor, UUID requestId, User assignee,
                                              RequestPriority priority, RequestStatus status,
                                              Instant now) {
        requireStaff(actor);
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(now, "now");
        ServiceRequest current = requireRequest(requestId);
        if (assignee != null) {
            requireStaff(assignee);
        }
        ServiceRequest updated = current;
        if (current.status() != status) {
            updated = current.transitionTo(status, now);
        } else if (now.isBefore(current.updatedAt())) {
            throw new IllegalArgumentException("update time must not precede updatedAt");
        }
        updated = new ServiceRequest(updated.requestId(), updated.studentId(), updated.category(),
                priority, updated.status(), assignee == null ? null : assignee.userId(),
                updated.createdAt(), now);
        save(updated);
        auditLog.record(actor.userId(), "SERVICE_REQUEST_UPDATED", "ServiceRequest",
                updated.requestId().toString(), now);
        return updated;
    }

    public List<AuditEvent> auditEvents() {
        return auditLog.events();
    }

    public synchronized List<ServiceRequest> forStudent(User actor, Student student) {
        requireSelfOrStaff(actor, student);
        return requests.values().stream()
                .filter(request -> request.studentId().equals(student.studentId()))
                .sorted(Comparator.comparing(ServiceRequest::createdAt).reversed())
                .toList();
    }

    public synchronized List<ServiceRequest> prioritizedWorkload() {
        return workload.stream().sorted(PRIORITY_ORDER).toList();
    }

    private void save(ServiceRequest request) {
        requests.put(request.requestId(), request);
        workload.removeIf(existing -> existing.requestId().equals(request.requestId()));
        if (request.status() == RequestStatus.SUBMITTED
                || request.status() == RequestStatus.IN_PROGRESS) {
            workload.add(request);
        }
    }

    private ServiceRequest requireRequest(UUID id) {
        ServiceRequest request = requests.get(id);
        if (request == null) {
            throw new IllegalArgumentException("Unknown service request: " + id);
        }
        return request;
    }

    private static void requireSelf(User actor, Student student) {
        Objects.requireNonNull(student, "student");
        if (actor == null || actor.status() != UserStatus.ACTIVE
                || actor.role() != UserRole.STUDENT
                || !actor.userId().equals(student.userId())) {
            throw new SecurityException("Only the active student may submit this request");
        }
    }

    private static void requireSelfOrStaff(User actor, Student student) {
        Objects.requireNonNull(student, "student");
        if (actor == null || actor.status() != UserStatus.ACTIVE
                || (actor.role() == UserRole.STUDENT
                && !actor.userId().equals(student.userId()))) {
            throw new SecurityException("Not authorized to view these requests");
        }
    }

    private static void requireStaff(User actor) {
        if (actor == null || actor.status() != UserStatus.ACTIVE
                || actor.role() == UserRole.STUDENT) {
            throw new SecurityException("An active staff user is required");
        }
    }
}
