package edu.nipa.scasp.domain;

public record CourseCapacityReport(String courseCode, String title, int capacity, int enrolled,
                                   int available, int waiting) {
}
