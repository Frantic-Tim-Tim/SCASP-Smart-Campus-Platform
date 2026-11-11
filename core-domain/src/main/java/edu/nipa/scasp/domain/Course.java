package edu.nipa.scasp.domain;

import java.util.Objects;

public record Course(String courseCode, String title, int capacity, CourseStatus status)
        implements Comparable<Course> {
    public Course {
        courseCode = User.requireText(courseCode, "courseCode");
        title = User.requireText(title, "title");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        Objects.requireNonNull(status, "status");
    }

    @Override
    public int compareTo(Course other) {
        return courseCode.compareTo(Objects.requireNonNull(other, "other").courseCode);
    }
}