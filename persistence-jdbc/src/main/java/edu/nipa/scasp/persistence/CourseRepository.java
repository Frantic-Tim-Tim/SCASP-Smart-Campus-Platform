package edu.nipa.scasp.persistence;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import edu.nipa.scasp.domain.Course;

public interface CourseRepository {

    void save(Course course) throws SQLException;

    Optional<Course> findByCode(String courseCode) throws SQLException;

    List<Course> findAll() throws SQLException;

    boolean update(Course course) throws SQLException;

    boolean deleteByCode(String courseCode) throws SQLException;
}
