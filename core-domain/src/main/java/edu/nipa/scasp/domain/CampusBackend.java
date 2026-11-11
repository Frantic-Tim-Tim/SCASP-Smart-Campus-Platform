package edu.nipa.scasp.domain;

import java.time.Instant;
import java.util.List;

public interface CampusBackend {
    User authenticate(String userId, char[] password);

    List<Course> listCourses(String query);

    Student getStudent(User actor, String studentId);

    RegistrationAttempt register(User actor, String studentId, String courseCode, Instant at);

    Registration withdraw(User actor, String studentId, String courseCode, Instant at);

    void createOrUpdateCourse(User actor, Course course);

    List<CourseCapacityReport> courseCapacityReport(User actor);
}
