package edu.nipa.scasp.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.WaitlistEntry;

public interface RegistrationRepository {

    RegistrationAttempt register(
            Connection connection,
            String studentId,
            String courseCode,
            Instant now
    ) throws SQLException;

    Registration withdraw(
            Connection connection,
            String studentId,
            String courseCode,
            Instant now
    ) throws SQLException;

    List<Registration> findByStudentId(
            Connection connection,
            String studentId
    ) throws SQLException;

    List<WaitlistEntry> findWaitlistByStudentId(
            Connection connection,
            String studentId
    ) throws SQLException;
}