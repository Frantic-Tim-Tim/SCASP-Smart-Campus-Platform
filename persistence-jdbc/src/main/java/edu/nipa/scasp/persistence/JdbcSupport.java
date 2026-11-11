package edu.nipa.scasp.persistence;

import java.sql.Timestamp;
import java.time.Instant;

final class JdbcSupport {
    private JdbcSupport() {
    }

    static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    static Instant instant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    static DataAccessException failure(String operation, Exception cause) {
        return new DataAccessException("Database operation failed: " + operation, cause);
    }
}
