package edu.nipa.scasp.domain;

import java.io.Serial;
import java.io.Serializable;

public record RegistrationKey(String studentId, String courseCode) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public RegistrationKey {
        studentId = User.requireText(studentId, "studentId");
        courseCode = User.requireText(courseCode, "courseCode");
    }
}
