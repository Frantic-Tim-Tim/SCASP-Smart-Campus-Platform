package edu.nipa.scasp.enterprise.repository;

import java.util.List;
import java.util.Optional;

import edu.nipa.scasp.domain.Course;

public interface CourseRepository {

    Optional<Course> findByCode(String courseCode);

    List<Course> findAll();

    boolean existsByCode(String courseCode);

    Course save(Course course);

    boolean deleteByCode(String courseCode);
}
