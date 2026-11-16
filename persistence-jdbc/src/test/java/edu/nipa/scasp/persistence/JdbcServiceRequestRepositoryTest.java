package edu.nipa.scasp.persistence;

import edu.nipa.scasp.domain.RequestPriority;
import edu.nipa.scasp.domain.RequestStatus;
import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcServiceRequestRepositoryTest {

    private JdbcServiceRequestRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "service_request_test_"
                + UUID.randomUUID().toString().replace("-", "");

        DatabaseConfig config = new DatabaseConfig(
                "jdbc:h2:mem:" + databaseName
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        ConnectionFactory factory = new ConnectionFactory(config);
        new DatabaseInitializer(factory).initialize();

        repository = new JdbcServiceRequestRepository(factory);

        try (Connection connection = factory.getConnection();
             PreparedStatement user = connection.prepareStatement("""
                     INSERT INTO users (user_id, name, role, status)
                     VALUES (?, ?, ?, ?)
                     """);
             PreparedStatement student = connection.prepareStatement("""
                     INSERT INTO students (
                         student_id, user_id, programme, year_of_study
                     )
                     VALUES (?, ?, ?, ?)
                     """)) {

            user.setString(1, "USER001");
            user.setString(2, "Test Student");
            user.setString(3, "STUDENT");
            user.setString(4, "ACTIVE");
            user.executeUpdate();

            student.setString(1, "STU001");
            student.setString(2, "USER001");
            student.setString(3, "Computer Science");
            student.setInt(4, 3);
            student.executeUpdate();
        }
    }

    @Test
    void createAndFindById() throws Exception {
        ServiceRequest request = newRequest();
        repository.create(request);

        Optional<ServiceRequest> result =
                repository.findById(request.requestId());

        assertTrue(result.isPresent());
        assertEquals(request, result.get());
    }

    @Test
    void findByStudentIdReturnsOnlyThatStudentsRequests()
            throws Exception {
        ServiceRequest request = newRequest();
        repository.create(request);

        List<ServiceRequest> results =
                repository.findByStudentId("STU001");

        assertEquals(1, results.size());
        assertEquals(request.requestId(), results.get(0).requestId());
    }

    @Test
    void findAllReturnsCreatedRequests() throws Exception {
        ServiceRequest first = newRequest();
        ServiceRequest second = newRequest();

        repository.create(first);
        repository.create(second);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void updatePersistsAssignmentAndStatusChanges()
            throws Exception {
        ServiceRequest request = newRequest();
        repository.create(request);

        Instant updatedAt = Instant.parse("2026-11-14T10:00:00Z");

        ServiceRequest updated = request
                .assignTo("STAFF001", updatedAt)
                .transitionTo(RequestStatus.IN_PROGRESS, updatedAt);

        assertTrue(repository.update(updated));

        ServiceRequest stored = repository
                .findById(request.requestId())
                .orElseThrow();

        assertEquals("STAFF001", stored.assignedTo());
        assertEquals(RequestStatus.IN_PROGRESS, stored.status());
        assertEquals(updatedAt, stored.updatedAt());
    }

    @Test
    void addUpdateAndFindUpdates() throws Exception {
        ServiceRequest request = newRequest();
        repository.create(request);

        ServiceRequestUpdate update = new ServiceRequestUpdate(
                UUID.randomUUID(),
                request.requestId(),
                "STAFF001",
                "Your request is being processed.",
                Instant.parse("2026-11-14T11:00:00Z")
        );

        repository.addUpdate(update);

        List<ServiceRequestUpdate> updates =
                repository.findUpdates(request.requestId());

        assertEquals(1, updates.size());
        assertEquals(update, updates.get(0));
    }

    @Test
    void findByIdReturnsEmptyForUnknownRequest()
            throws Exception {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    private ServiceRequest newRequest() {
        Instant now = Instant.parse("2026-11-14T08:00:00Z");

        return new ServiceRequest(
                UUID.randomUUID(),
                "STU001",
                "Unable to access student portal",
                "The student portal rejects my login credentials.",
                "IT SUPPORT",
                RequestPriority.HIGH,
                RequestStatus.SUBMITTED,
                null,
                now,
                now
        );
    }
}
