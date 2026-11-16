package edu.nipa.scasp.enterprise.bean;

import edu.nipa.scasp.domain.Announcement;
import edu.nipa.scasp.domain.AnnouncementAudience;
import edu.nipa.scasp.domain.AnnouncementNotification;
import edu.nipa.scasp.enterprise.repository.AnnouncementRepository;
import edu.nipa.scasp.persistence.ConnectionFactory;
import edu.nipa.scasp.persistence.JdbcAnnouncementRepository;

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
@Local(AnnouncementRepository.class)
public class JdbcAnnouncementRepositoryAdapter
        implements AnnouncementRepository {

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcAnnouncementRepository delegate;

    @PostConstruct
    void initialize() {
        delegate = new JdbcAnnouncementRepository(
                new ConnectionFactory(dataSource)
        );
    }

    @Override
    public void create(Announcement announcement) {
        execute(() -> {
            delegate.create(announcement);
            return null;
        });
    }

    @Override
    public Optional<Announcement> findById(UUID announcementId) {
        return execute(() -> delegate.findById(announcementId));
    }

    @Override
    public List<Announcement> findAll() {
        return execute(delegate::findAll);
    }

    @Override
    public List<AnnouncementNotification> findForUser(
            String userId,
            AnnouncementAudience audience
    ) {
        return execute(() -> delegate.findForUser(userId, audience));
    }

    @Override
    public boolean update(Announcement announcement) {
        return execute(() -> delegate.update(announcement));
    }

    @Override
    public boolean markRead(UUID announcementId, String userId) {
        return execute(() -> delegate.markRead(announcementId, userId));
    }

    private <T> T execute(SqlOperation<T> operation) {
        try {
            return operation.run();
        } catch (SQLException exception) {
            throw new EJBException(
                    "Announcement database operation failed",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}
