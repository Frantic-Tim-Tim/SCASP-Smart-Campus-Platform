package edu.nipa.scasp.domain;

import java.util.Objects;

public record User(String userId, String name, UserRole role, UserStatus status) {
    public User {
        userId = requireText(userId, "userId");
        name = requireText(name, "name");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
    }

    static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}