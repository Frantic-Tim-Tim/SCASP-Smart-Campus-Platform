package edu.nipa.scasp.enterprise.bean;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import edu.nipa.scasp.enterprise.service.RegistrationConflictException;
import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.domain.WaitlistEntry;

import edu.nipa.scasp.enterprise.repository.StudentProfileRepository;
import edu.nipa.scasp.enterprise.service.CourseRegistrationService;
import edu.nipa.scasp.enterprise.service.UserManagementService;

import edu.nipa.scasp.persistence.JdbcRegistrationRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@Local(CourseRegistrationService.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CourseRegistrationBean
        implements CourseRegistrationService {

    @EJB
    private UserManagementService userManagementService;

    @EJB
    private StudentProfileRepository studentProfileRepository;

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcRegistrationRepository registrationRepository;

    public CourseRegistrationBean() {
    }

    @PostConstruct
    public void initialize() {
        registrationRepository = new JdbcRegistrationRepository();
    }

    @Override
    public RegistrationAttempt registerForCourse(
            String authenticatedUserId,
            String courseCode
    ) {
        Student student = requireStudent(authenticatedUserId);

        try (Connection connection = dataSource.getConnection()) {

            return registrationRepository.register(
            connection,
            student.studentId(),
            courseCode,
            Instant.now()
            );

        } catch (IllegalStateException exception) {
        throw new RegistrationConflictException(
            exception.getMessage(),
            exception
            );
        } catch (SQLException exception) {
        throw new EJBException(
            "Unable to register student for course",
            exception
            );
        }
    }

    @Override
    public Registration withdrawFromCourse(
            String authenticatedUserId,
            String courseCode
    ) {
        Student student = requireStudent(authenticatedUserId);

        try (Connection connection = dataSource.getConnection()) {

            return registrationRepository.withdraw(
            connection,
            student.studentId(),
            courseCode,
            Instant.now()
            );

        } catch (IllegalStateException exception) {
        throw new RegistrationConflictException(
            exception.getMessage(),
            exception
            );
        } catch (SQLException exception) {
        throw new EJBException(
            "Unable to withdraw student from course",
            exception
            );
        }
    }

    @Override
    public List<Registration> getMyRegistrations(
            String authenticatedUserId
    ) {
        Student student = requireStudent(authenticatedUserId);

        try (Connection connection = dataSource.getConnection()) {

            return registrationRepository.findByStudentId(
                    connection,
                    student.studentId()
            );

        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve student registrations",
                    exception
            );
        }
    }

    @Override
    public List<WaitlistEntry> getMyWaitlistEntries(
            String authenticatedUserId
    ) {
        Student student = requireStudent(authenticatedUserId);

        try (Connection connection = dataSource.getConnection()) {

            return registrationRepository.findWaitlistByStudentId(
                    connection,
                    student.studentId()
            );

        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to retrieve student waitlist entries",
                    exception
            );
        }
    }

    private Student requireStudent(String userId) {

        Objects.requireNonNull(userId, "authenticatedUserId");

        if (userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user ID must not be blank"
            );
        }

        User user = userManagementService.getUserById(userId);

        if (user.role() != UserRole.STUDENT) {
            throw new SecurityException(
                    "Only student accounts can access course registration"
            );
        }

        if (user.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "The student account is not active"
            );
        }

        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Student profile not found for user: " + userId
                ));
    }
}