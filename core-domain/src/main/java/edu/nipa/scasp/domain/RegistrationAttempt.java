package edu.nipa.scasp.domain;

import java.util.Objects;

public record RegistrationAttempt(Registration registration, WaitlistEntry waitlistEntry) {
    public RegistrationAttempt {
        if ((registration == null) == (waitlistEntry == null)) {
            throw new IllegalArgumentException("exactly one outcome must be present");
        }
    }

    public boolean isWaitlisted() {
        return waitlistEntry != null;
    }
}