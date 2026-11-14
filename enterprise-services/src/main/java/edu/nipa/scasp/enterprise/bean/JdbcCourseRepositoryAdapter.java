package edu.nipa.scasp.enterprise.bean;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.enterprise.repository.CourseRepository;
import edu.nipa.scasp.persistence.ConnectionFactory;
import edu.nipa.scasp.persistence.JdbcCourseRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(CourseRepository.class)
public class JdbcCourseRepositoryAdapter implements CourseRepository {

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcCourseRepository jdbcRepository;

    @PostConstruct
    public void initialize() {
        jdbcRepository = new JdbcCourseRepository(
                new ConnectionFactory(dataSource)
        );
    }

    @Override
    public Optional<Course> findByCode(String courseCode) {
        try {
            return jdbcRepository.findByCode(courseCode);
        } catch (SQLException exception) {
            throw new EJBException("Could not retrieve course", exception);
        }
    }

    @Override
    public List<Course> findAll() {
        try {
            return jdbcRepository.findAll();
        } catch (SQLException exception) {
            throw new EJBException("Could not retrieve courses", exception);
        }
    }

    @Override
    public boolean existsByCode(String courseCode) {
        return findByCode(courseCode).isPresent();
    }

    @Override
    public Course save(Course course) {
        try {
            if (jdbcRepository.findByCode(course.courseCode()).isPresent()) {
                jdbcRepository.update(course);
            } else {
                jdbcRepository.save(course);
            }

            return course;
        } catch (SQLException exception) {
            throw new EJBException("Could not save course", exception);
        }
    }

    @Override
    public boolean deleteByCode(String courseCode) {
        try {
            return jdbcRepository.deleteByCode(courseCode);
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new IllegalStateException(
                        "Course cannot be deleted while registrations or waitlist entries exist",
                        exception
                );
            }

            throw new EJBException("Could not delete course", exception);
        }
    }
}
