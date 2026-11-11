package edu.nipa.scasp.enterprise;

import jakarta.ejb.Local;

@Local
public interface SharedMetricsLocal {
    long successfulRegistrations();

    void recordSuccessfulRegistration();

    long timerExecutions();

    void recordTimerExecution();
}
