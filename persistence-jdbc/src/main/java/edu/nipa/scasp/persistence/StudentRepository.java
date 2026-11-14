package edu.nipa.scasp.persistence;

import java.sql.SQLException;
import java.util.Optional;

import edu.nipa.scasp.domain.Student;

public interface StudentRepository {

    Optional<Student> findByUserId(String userId) throws SQLException;

    Optional<Student> findByStudentId(String studentId) throws SQLException;
}