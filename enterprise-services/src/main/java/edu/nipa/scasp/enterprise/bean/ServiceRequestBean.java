package edu.nipa.scasp.enterprise.bean;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.repository.ServiceRequestRepository;
import edu.nipa.scasp.enterprise.repository.StudentProfileRepository;
import edu.nipa.scasp.enterprise.service.ServiceRequestService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Stateless
@Local(ServiceRequestService.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ServiceRequestBean implements ServiceRequestService {

    @EJB
    private ServiceRequestRepository requestRepository;

    @EJB
    private StudentProfileRepository studentProfileRepository;

    @EJB
    private UserManagementService userManagementService;

    @Override
    public ServiceRequest submit(
            String authenticatedUserId,
            String title,
            String description,
            String category,
            RequestPriority priority
    ) {
        Student student = requireStudent(authenticatedUserId);
        Instant now = Instant.now();

        ServiceRequest request = new ServiceRequest(
                UUID.randomUUID(),
                student.studentId(),
                title,
                description,
                category,
                Objects.requireNonNull(priority, "priority"),
                RequestStatus.SUBMITTED,
                null,
                now,
                now
        );

        requestRepository.create(request);

        recordUpdate(
                request.requestId(),
                authenticatedUserId,
                "Request submitted."
        );

        return request;
    }

    @Override
    public List<ServiceRequest> getMyRequests(String authenticatedUserId) {
        Student student = requireStudent(authenticatedUserId);

        return requestRepository.findByStudentId(student.studentId());
    }

    @Override
    public ServiceRequest getMyRequest(
            String authenticatedUserId,
            UUID requestId
    ) {
        User actor = requireActiveUser(authenticatedUserId);
        ServiceRequest request = findRequest(requestId);

        if (actor.role() == UserRole.STUDENT) {
            Student student = requireStudent(authenticatedUserId);

            if (!request.studentId().equals(student.studentId())) {
                throw new SecurityException(
                        "You are not allowed to view this request"
                );
            }
        } else {
            requireStaffOrAdmin(actor);
        }

        return request;
    }

    @Override
    public List<ServiceRequest> getAllRequests(String authenticatedUserId) {
        requireStaffOrAdmin(requireActiveUser(authenticatedUserId));

        return requestRepository.findAll();
    }

    @Override
    public ServiceRequest assign(
            String authenticatedUserId,
            UUID requestId,
            String staffUserId
    ) {
        requireStaffOrAdmin(requireActiveUser(authenticatedUserId));

        User assignee = requireActiveUser(staffUserId);

        if (assignee.role() != UserRole.STAFF
                && assignee.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "Requests can only be assigned to staff or administrators"
            );
        }

        ServiceRequest request = findRequest(requestId);
        Instant now = nextTimestamp(request.updatedAt());

        ServiceRequest updated = request.assignTo(staffUserId, now);

        requestRepository.update(updated);

        recordUpdate(
                requestId,
                authenticatedUserId,
                "Request assigned to " + staffUserId + "."
        );

        return updated;
    }

    @Override
    public ServiceRequest changePriority(
            String authenticatedUserId,
            UUID requestId,
            RequestPriority priority
    ) {
        requireStaffOrAdmin(requireActiveUser(authenticatedUserId));

        ServiceRequest request = findRequest(requestId);
        Instant now = nextTimestamp(request.updatedAt());

        ServiceRequest updated = request.changePriority(
                Objects.requireNonNull(priority, "priority"),
                now
        );

        requestRepository.update(updated);

        recordUpdate(
                requestId,
                authenticatedUserId,
                "Priority changed from " + request.priority()
                        + " to " + priority + "."
        );

        return updated;
    }

    @Override
    public ServiceRequest changeStatus(
            String authenticatedUserId,
            UUID requestId,
            RequestStatus status
    ) {
        requireStaffOrAdmin(requireActiveUser(authenticatedUserId));

        ServiceRequest request = findRequest(requestId);
        Instant now = nextTimestamp(request.updatedAt());

        ServiceRequest updated = request.transitionTo(
                Objects.requireNonNull(status, "status"),
                now
        );

        requestRepository.update(updated);

        recordUpdate(
                requestId,
                authenticatedUserId,
                "Status changed from " + request.status()
                        + " to " + status + "."
        );

        return updated;
    }

    @Override
    public ServiceRequestUpdate addProgressUpdate(
            String authenticatedUserId,
            UUID requestId,
            String note
    ) {
        requireStaffOrAdmin(requireActiveUser(authenticatedUserId));

        ServiceRequest request = findRequest(requestId);
        String validatedNote = requireText(note, "note");
        Instant now = nextTimestamp(request.updatedAt());

        ServiceRequest updated = copyWithUpdatedAt(request, now);

        requestRepository.update(updated);

        return recordUpdate(
                requestId,
                authenticatedUserId,
                validatedNote,
                now
        );
    }

    @Override
    public List<ServiceRequestUpdate> getUpdates(
            String authenticatedUserId,
            UUID requestId
    ) {
        getMyRequest(authenticatedUserId, requestId);

        return requestRepository.findUpdates(requestId);
    }

    private Student requireStudent(String userId) {
        User user = requireActiveUser(userId);

        if (user.role() != UserRole.STUDENT) {
            throw new SecurityException(
                    "Only students can perform this operation"
            );
        }

        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Student profile not found"
                ));
    }

    private User requireActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("Authentication is required");
        }

        User user = userManagementService.getUserById(userId);

        if (user == null) {
            throw new SecurityException("Authenticated user was not found");
        }

        if (user.status() != UserStatus.ACTIVE) {
            throw new SecurityException("Your account is not active");
        }

        return user;
    }

    private void requireStaffOrAdmin(User user) {
        if (user.role() != UserRole.STAFF
                && user.role() != UserRole.ADMIN) {
            throw new SecurityException(
                    "Only staff and administrators can perform this operation"
            );
        }
    }

    private ServiceRequest findRequest(UUID requestId) {
        Objects.requireNonNull(requestId, "requestId");

        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Service request not found: " + requestId
                ));
    }

    private void recordUpdate(
            UUID requestId,
            String authorUserId,
            String note
    ) {
        recordUpdate(requestId, authorUserId, note, Instant.now());
    }

    private ServiceRequestUpdate recordUpdate(
            UUID requestId,
            String authorUserId,
            String note,
            Instant createdAt
    ) {
        ServiceRequestUpdate update = new ServiceRequestUpdate(
                UUID.randomUUID(),
                requestId,
                authorUserId,
                requireText(note, "note"),
                createdAt
        );

        requestRepository.addUpdate(update);

        return update;
    }

    private ServiceRequest copyWithUpdatedAt(
            ServiceRequest request,
            Instant updatedAt
    ) {
        return new ServiceRequest(
                request.requestId(),
                request.studentId(),
                request.title(),
                request.description(),
                request.category(),
                request.priority(),
                request.status(),
                request.assignedTo(),
                request.createdAt(),
                updatedAt
        );
    }

    private Instant nextTimestamp(Instant previous) {
        Instant now = Instant.now();

        return now.isAfter(previous)
                ? now
                : previous.plusNanos(1);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }
}
