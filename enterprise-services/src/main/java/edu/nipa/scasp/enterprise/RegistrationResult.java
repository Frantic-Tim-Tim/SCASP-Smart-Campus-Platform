package edu.nipa.scasp.enterprise;

import java.io.Serial;
import java.io.Serializable;

public record RegistrationResult(String outcome, String registrationId, int waitlistPosition)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
