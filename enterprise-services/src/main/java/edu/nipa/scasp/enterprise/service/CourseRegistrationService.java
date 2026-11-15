package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.domain.Registration;
import edu.nipa.scasp.domain.RegistrationAttempt;
import edu.nipa.scasp.domain.WaitlistEntry;

public interface CourseRegistrationService {

    RegistrationAttempt registerForCourse(
            String authenticatedUserId,
            String courseCode
    );

    Registration withdrawFromCourse(
            String authenticatedUserId,
            String courseCode
    );

    List<Registration> getMyRegistrations(
            String authenticatedUserId
    );

    List<WaitlistEntry> getMyWaitlistEntries(
            String authenticatedUserId
    );
}