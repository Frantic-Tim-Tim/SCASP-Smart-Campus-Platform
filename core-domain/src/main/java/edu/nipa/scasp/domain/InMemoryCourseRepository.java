package edu.nipa.scasp.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCourseRepository implements CourseRepository {
    private final Map<String, Course> courses = new HashMap<>();

    @Override
    public synchronized Optional<Course> findById(String id) {
        return Optional.ofNullable(courses.get(User.requireText(id, "id")));
    }

    @Override
    public synchronized List<Course> findAll() {
        return findAll(Comparator.comparing(Course::courseCode));
    }

    @Override
    public synchronized List<Course> findAll(Comparator<Course> ordering) {
        return courses.values().stream().sorted(ordering).toList();
    }

    @Override
    public synchronized List<Course> search(String query) {
        String term = User.requireText(query, "query").toLowerCase(Locale.ROOT);
        return courses.values().stream()
                .filter(course -> course.courseCode().toLowerCase(Locale.ROOT).contains(term)
                        || course.title().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(Course::courseCode))
                .toList();
    }

    @Override
    public synchronized void save(String id, Course course) {
        if (!User.requireText(id, "id").equals(course.courseCode())) {
            throw new IllegalArgumentException("Repository key must match courseCode");
        }
        courses.put(id, course);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        return courses.remove(User.requireText(id, "id")) != null;
    }
}
