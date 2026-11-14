package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.domain.Course;
import edu.nipa.scasp.domain.User;

public interface CourseManagementService {

    Course createCourse(User actor, Course course);

    Course getCourse(User actor, String courseCode);

    List<Course> getCourses(
            User actor,
            String search,
            String sortBy,
            String direction
    );

    Course updateCourse(User actor, String courseCode, Course course);

    boolean deleteCourse(User actor, String courseCode);
}
