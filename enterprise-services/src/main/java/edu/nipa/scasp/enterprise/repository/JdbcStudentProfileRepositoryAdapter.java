package edu.nipa.scasp.enterprise.repository;

import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.persistence.ConnectionFactory;
import edu.nipa.scasp.persistence.JdbcStudentRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(StudentProfileRepository.class)
public class JdbcStudentProfileRepositoryAdapter
        implements StudentProfileRepository {

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcStudentRepository jdbcRepository;

    @PostConstruct
    public void initialize() {
        jdbcRepository = new JdbcStudentRepository(
                new ConnectionFactory(dataSource)
        );
    }

    @Override
    public Optional<Student> findByUserId(String userId) {
        try {
            return jdbcRepository.findByUserId(userId);
        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve student profile for user: " + userId,
                    exception
            );
        }
    }

    @Override
    public Optional<Student> findByStudentId(String studentId) {
        try {
            return jdbcRepository.findByStudentId(studentId);
        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve student profile: " + studentId,
                    exception
            );
        }
    }
}