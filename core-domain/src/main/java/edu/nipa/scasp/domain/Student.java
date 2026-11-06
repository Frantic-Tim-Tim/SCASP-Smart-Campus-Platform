package edu.nipa.scasp.domain;

import java.util.Objects;

public record Student(String studentId, String userId, String programme, int yearOfStudy) {
    public Student {
        studentId = User.requireText(studentId, "studentId");
        userId = User.requireText(userId, "userId");
        programme = User.requireText(programme, "programme");
        if (yearOfStudy < 1) {
            throw new IllegalArgumentException("yearOfStudy must be positive");
        }
    }
}