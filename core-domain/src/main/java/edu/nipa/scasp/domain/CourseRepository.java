package edu.nipa.scasp.domain;

import java.util.Comparator;
import java.util.List;

public interface CourseRepository extends Repository<String, Course> {
    List<Course> search(String query);

    List<Course> findAll(Comparator<Course> ordering);
}
