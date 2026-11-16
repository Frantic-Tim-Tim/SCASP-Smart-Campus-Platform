package edu.nipa.scasp.enterprise.service;

import java.util.List;

import edu.nipa.scasp.persistence.CourseEnrollmentReport;
import edu.nipa.scasp.persistence.StudentRegistrationReport;
import edu.nipa.scasp.persistence.CourseWaitlistReport;

public interface AcademicReportsService {

    List<CourseEnrollmentReport> getCourseEnrollmentSummary(String actorUserId);

    List<StudentRegistrationReport> getStudentRegistrations(String actorUserId);

    List<CourseWaitlistReport> getCourseWaitlist(String actorUserId);
}
