package edu.nipa.scasp.domain;

import java.util.Objects;
import java.util.UUID;

public record CampusService(UUID serviceId, String name, String description,
                            double latitude, double longitude) {
    public CampusService {
        Objects.requireNonNull(serviceId, "serviceId");
        name = User.requireText(name, "name");
        description = User.requireText(description, "description");
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}