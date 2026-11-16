package edu.nipa.scasp.enterprise.bean;

import edu.nipa.scasp.domain.ServiceRequest;
import edu.nipa.scasp.domain.ServiceRequestUpdate;
import edu.nipa.scasp.enterprise.repository.ServiceRequestRepository;
import edu.nipa.scasp.persistence.ConnectionFactory;
import edu.nipa.scasp.persistence.JdbcServiceRequestRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
@Local(ServiceRequestRepository.class)
public class JdbcServiceRequestRepositoryAdapter
        implements ServiceRequestRepository {

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcServiceRequestRepository delegate;

    @PostConstruct
    void initialize() {
        delegate = new JdbcServiceRequestRepository(
                new ConnectionFactory(dataSource)
        );
    }

    @Override
    public void create(ServiceRequest request) {
        execute(() -> { delegate.create(request); return null; });
    }

    @Override
    public Optional<ServiceRequest> findById(UUID requestId) {
        return execute(() -> delegate.findById(requestId));
    }

    @Override
    public List<ServiceRequest> findByStudentId(String studentId) {
        return execute(() -> delegate.findByStudentId(studentId));
    }

    @Override
    public List<ServiceRequest> findAll() {
        return execute(delegate::findAll);
    }

    @Override
    public boolean update(ServiceRequest request) {
        return execute(() -> delegate.update(request));
    }

    @Override
    public void addUpdate(ServiceRequestUpdate update) {
        execute(() -> { delegate.addUpdate(update); return null; });
    }

    @Override
    public List<ServiceRequestUpdate> findUpdates(UUID requestId) {
        return execute(() -> delegate.findUpdates(requestId));
    }

    private <T> T execute(SqlOperation<T> operation) {
        try {
            return operation.run();
        } catch (SQLException exception) {
            throw new EJBException(
                    "Service request database operation failed",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}
