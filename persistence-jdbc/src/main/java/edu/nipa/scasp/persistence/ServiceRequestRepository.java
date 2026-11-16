package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository {

    void create(ServiceRequest request) throws SQLException;

    Optional<ServiceRequest> findById(UUID requestId) throws SQLException;

    List<ServiceRequest> findByStudentId(String studentId)
            throws SQLException;

    List<ServiceRequest> findAll() throws SQLException;

    boolean update(ServiceRequest request) throws SQLException;

    void addUpdate(ServiceRequestUpdate update) throws SQLException;

    List<ServiceRequestUpdate> findUpdates(UUID requestId)
            throws SQLException;
}
