package edu.nipa.scasp.enterprise.service;

import edu.nipa.scasp.domain.Student;
import edu.nipa.scasp.domain.User;

public interface StudentAccountProvisioningService {

    Student createStudentAccount(
            User actor,
            String userId,
            String name,
            String studentId,
            String programme,
            int yearOfStudy,
            char[] password
    );
}