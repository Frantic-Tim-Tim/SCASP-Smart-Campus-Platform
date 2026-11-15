package edu.nipa.scasp.enterprise.bean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

import javax.sql.DataSource;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;
import edu.nipa.scasp.domain.UserRole;
import edu.nipa.scasp.domain.UserStatus;
import edu.nipa.scasp.enterprise.security.PasswordHashService;
import edu.nipa.scasp.enterprise.service.StudentAccountProvisioningService;
import edu.nipa.scasp.enterprise.service.UserManagementService;
import edu.nipa.scasp.persistence.JdbcStudentAccountProvisioner;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@Local(StudentAccountProvisioningService.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StudentAccountProvisioningBean
        implements StudentAccountProvisioningService {

    @EJB
    private UserManagementService userManagementService;

    @Resource(lookup = "java:/jdbc/ScaspDS")
    private DataSource dataSource;

    private JdbcStudentAccountProvisioner provisioner;

    @PostConstruct
    void initialize() {
        provisioner = new JdbcStudentAccountProvisioner();
    }

    @Override
    public Student createStudentAccount(
            User actor,
            String userId,
            String name,
            String studentId,
            String programme,
            int yearOfStudy,
            char[] password
    ) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(password, "password");

        try {
            // Reload the actor instead of trusting a supplied User object.
            User currentActor =
                    userManagementService.getUserById(actor.userId());

            requireActiveAdministrator(currentActor);

            User studentUser = new User(
                    userId,
                    name,
                    UserRole.STUDENT,
                    UserStatus.ACTIVE
            );

            Student student = new Student(
                    studentId,
                    userId,
                    programme,
                    yearOfStudy
            );

            String passwordHash = PasswordHashService.hash(password);

            try (Connection connection = dataSource.getConnection()) {
                return provisioner.create(
                        connection,
                        studentUser,
                        student,
                        passwordHash
                );
            }

        } catch (SQLException exception) {
            throw new EJBException(
                    "Unable to create the student account",
                    exception
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void requireActiveAdministrator(User actor) {
        if (actor.role() != UserRole.ADMIN
                || actor.status() != UserStatus.ACTIVE) {
            throw new SecurityException(
                    "An active administrator is required"
            );
        }
    }
}