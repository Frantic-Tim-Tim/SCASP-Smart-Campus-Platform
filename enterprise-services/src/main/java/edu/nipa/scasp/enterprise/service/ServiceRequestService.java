package edu.nipa.scasp.enterprise.service;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestService {

    ServiceRequest submit(
            String authenticatedUserId,
            String title,
            String description,
            String category,
            RequestPriority priority
    );

    List<ServiceRequest> getMyRequests(String authenticatedUserId);

    ServiceRequest getMyRequest(
            String authenticatedUserId,
            UUID requestId
    );

    List<ServiceRequest> getAllRequests(String authenticatedUserId);

    ServiceRequest assign(
            String authenticatedUserId,
            UUID requestId,
            String staffUserId
    );

    ServiceRequest changePriority(
            String authenticatedUserId,
            UUID requestId,
            RequestPriority priority
    );

    ServiceRequest changeStatus(
            String authenticatedUserId,
            UUID requestId,
            RequestStatus status
    );

    ServiceRequestUpdate addProgressUpdate(
            String authenticatedUserId,
            UUID requestId,
            String note
    );

    List<ServiceRequestUpdate> getUpdates(
            String authenticatedUserId,
            UUID requestId
    );
}
