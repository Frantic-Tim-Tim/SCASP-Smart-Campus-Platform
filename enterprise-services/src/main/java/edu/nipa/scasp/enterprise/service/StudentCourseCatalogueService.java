package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.domain.Course;

public interface StudentCourseCatalogueService {

    List<Course> getAvailableCourses(String authenticatedUserId, String search);
}