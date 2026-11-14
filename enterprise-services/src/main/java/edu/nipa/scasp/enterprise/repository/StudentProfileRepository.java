package edu.nipa.scasp.enterprise.repository;

import java.util.Optional;

import edu.nipa.scasp.domain.Student;

public interface StudentProfileRepository {

    Optional<Student> findByUserId(String userId);

    Optional<Student> findByStudentId(String studentId);
}