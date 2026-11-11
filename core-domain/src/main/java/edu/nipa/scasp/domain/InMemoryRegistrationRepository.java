package edu.nipa.scasp.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRegistrationRepository implements RegistrationRepository {
    private final Map<RegistrationKey, Registration> registrations = new HashMap<>();

    @Override
    public synchronized Optional<Registration> findById(RegistrationKey key) {
        return Optional.ofNullable(registrations.get(key));
    }

    @Override
    public synchronized List<Registration> findAll() {
        return List.copyOf(registrations.values());
    }

    @Override
    public synchronized void save(RegistrationKey key, Registration registration) {
        if (!key.studentId().equals(registration.studentId())
                || !key.courseCode().equals(registration.courseCode())) {
            throw new IllegalArgumentException("Repository key must match registration identity");
        }
        registrations.put(key, registration);
    }

    @Override
    public synchronized boolean deleteById(RegistrationKey key) {
        return registrations.remove(key) != null;
    }
}
