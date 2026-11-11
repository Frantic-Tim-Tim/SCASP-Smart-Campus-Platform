package edu.nipa.scasp.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryStudentRepository implements StudentRepository {
    private final Map<String, Student> students = new HashMap<>();

    @Override
    public synchronized Optional<Student> findById(String id) {
        return Optional.ofNullable(students.get(User.requireText(id, "id")));
    }

    @Override
    public synchronized List<Student> findAll() {
        return List.copyOf(students.values());
    }

    @Override
    public synchronized void save(String id, Student student) {
        if (!User.requireText(id, "id").equals(student.studentId())) {
            throw new IllegalArgumentException("Repository key must match studentId");
        }
        students.put(id, student);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        return students.remove(User.requireText(id, "id")) != null;
    }
}
