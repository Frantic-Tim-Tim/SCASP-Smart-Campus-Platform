package edu.nipa.scasp.enterprise;

import jakarta.ejb.Local;

@Local
public interface RegistrationLocal {
    RegistrationResult register(String studentId, String courseCode);
}
