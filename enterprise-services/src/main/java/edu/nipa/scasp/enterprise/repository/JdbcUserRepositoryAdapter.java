package edu.nipa.scasp.enterprise.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.persistence.ConnectionFactory;
import edu.nipa.scasp.persistence.JdbcUserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(UserRepository.class)
public class JdbcUserRepositoryAdapter implements UserRepository {

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcUserRepository jdbcRepository;

    /**
     * Initializes the JDBC repository using WildFly's
     * managed MariaDB datasource.
     */
    @PostConstruct
    public void initialize() {
        ConnectionFactory connectionFactory =
                new ConnectionFactory(dataSource);

        jdbcRepository =
                new JdbcUserRepository(connectionFactory);
    }

    @Override
    public Optional<User> findById(String userId) {
        try {
            return jdbcRepository.findById(userId);
        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve user: " + userId,
                    exception
            );
        }
    }

    @Override
    public List<User> findAll() {
        try {
            return jdbcRepository.findAll();
        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve users",
                    exception
            );
        }
    }

    @Override
    public boolean existsById(String userId) {
        return findById(userId).isPresent();
    }

    /**
     * Inserts a new user or updates an existing user.
     */
    @Override
    public User save(User user) {
        try {
            Optional<User> existing =
                    jdbcRepository.findById(user.userId());

            if (existing.isPresent()) {
                boolean updated = jdbcRepository.update(user);

                if (!updated) {
                    throw new EJBException(
                            "User could not be updated: "
                            + user.userId()
                    );
                }
            } else {
                jdbcRepository.save(user);
            }

            return user;

        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to save user: " + user.userId(),
                    exception
            );
        }
    }
}