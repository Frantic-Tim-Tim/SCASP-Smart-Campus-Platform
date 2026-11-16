package edu.nipa.scasp.enterprise.bean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.service.AcademicReportsService;
import edu.nipa.scasp.enterprise.service.UserManagementService;
import edu.nipa.scasp.persistence.CourseEnrollmentReport;
import edu.nipa.scasp.persistence.CourseWaitlistReport;
import edu.nipa.scasp.persistence.JdbcAcademicReportsRepository;
import edu.nipa.scasp.persistence.StudentRegistrationReport;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless
@Local(AcademicReportsService.class)
public class AcademicReportsBean implements AcademicReportsService {

    @EJB
    private UserManagementService userManagementService;

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private final JdbcAcademicReportsRepository reportsRepository =
            new JdbcAcademicReportsRepository();

    public AcademicReportsBean() {
    }

    @Override
    public List<CourseEnrollmentReport> getCourseEnrollmentSummary(
            String actorUserId
    ) {
        authorizeAdministrator(actorUserId);

        try (Connection connection = dataSource.getConnection()) {
            return reportsRepository.findCourseEnrollmentSummary(connection);
        } catch (SQLException exception) {
            throw new EJBException("Unable to retrieve course enrolment report", exception);
        }
    }

    @Override
    public List<StudentRegistrationReport> getStudentRegistrations(
            String actorUserId
    ) {
        authorizeAdministrator(actorUserId);

        try (Connection connection = dataSource.getConnection()) {
            return reportsRepository.findStudentRegistrations(connection);
        } catch (SQLException exception) {
            throw new EJBException("Unable to retrieve student registration report", exception);
        }
    }

    @Override
    public List<CourseWaitlistReport> getCourseWaitlist(String actorUserId) {
        authorizeAdministrator(actorUserId);

        try (Connection connection = dataSource.getConnection()) {
            return reportsRepository.findCourseWaitlist(connection);
        } catch (SQLException exception) {
            throw new EJBException("Unable to retrieve course waitlist report", exception);
        }
    }

    private void authorizeAdministrator(String actorUserId) {
        Objects.requireNonNull(actorUserId, "actorUserId");

        if (actorUserId.isBlank()) {
            throw new SecurityException("An authenticated administrator is required");
        }

        User actor = userManagementService.getUserById(actorUserId);

        if (actor.role() != UserRole.ADMIN || actor.status() != UserStatus.ACTIVE) {
            throw new SecurityException("Only active administrators can access academic reports");
        }
    }
}
