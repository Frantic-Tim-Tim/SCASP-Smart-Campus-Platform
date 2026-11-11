package edu.nipa.scasp.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryUserRepository implements Repository<String, User> {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public synchronized Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(User.requireText(id, "id")));
    }

    @Override
    public synchronized List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public synchronized void save(String id, User user) {
        if (!User.requireText(id, "id").equals(user.userId())) {
            throw new IllegalArgumentException("Repository key must match userId");
        }
        users.put(id, user);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        return users.remove(User.requireText(id, "id")) != null;
    }
}
