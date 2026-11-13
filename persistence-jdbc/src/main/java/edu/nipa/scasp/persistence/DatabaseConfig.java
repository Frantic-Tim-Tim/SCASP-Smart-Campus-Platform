package edu.nipa.scasp.persistence;

import java.util.Objects;

public record DatabaseConfig(String url, String username, String password) {

    public DatabaseConfig {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");

        if (url.isBlank()) {
            throw new IllegalArgumentException("Database URL must not be blank");
        }
    }

    public static DatabaseConfig fromEnvironment() {
        return new DatabaseConfig(
                requiredEnvironmentVariable("SCASP_DB_URL"),
                requiredEnvironmentVariable("SCASP_DB_USER"),
                requiredEnvironmentVariable("SCASP_DB_PASSWORD")
        );
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable is missing: " + name
            );
        }

        return value;
    }
}