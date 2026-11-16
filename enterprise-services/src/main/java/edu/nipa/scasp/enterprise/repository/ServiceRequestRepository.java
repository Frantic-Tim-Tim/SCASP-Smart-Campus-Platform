package edu.nipa.scasp.enterprise.repository;

import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository {

    void create(ServiceRequest request);

    Optional<ServiceRequest> findById(UUID requestId);

    List<ServiceRequest> findByStudentId(String studentId);

    List<ServiceRequest> findAll();

    boolean update(ServiceRequest request);

    void addUpdate(ServiceRequestUpdate update);

    List<ServiceRequestUpdate> findUpdates(UUID requestId);
}
