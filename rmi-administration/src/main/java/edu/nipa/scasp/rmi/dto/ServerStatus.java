package edu.nipa.scasp.rmi.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Transfers administration server status information to remote clients.
 */
public record ServerStatus(
        String serverName,
        String status,
        Instant checkedAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ServerStatus {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(checkedAt, "checkedAt");

        if (serverName.isBlank()) {
            throw new IllegalArgumentException(
                    "serverName must not be blank"
            );
        }

        if (status.isBlank()) {
            throw new IllegalArgumentException(
                    "status must not be blank"
            );
        }
    }
}