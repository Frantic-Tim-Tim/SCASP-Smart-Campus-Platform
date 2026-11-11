package edu.nipa.scasp.enterprise;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

@Singleton
@Startup
public class SharedMetricsBean implements SharedMetricsLocal {
    private static final Logger LOGGER = Logger.getLogger(SharedMetricsBean.class.getName());
    private final LongAdder registrations = new LongAdder();
    private final LongAdder timerExecutions = new LongAdder();

    @PostConstruct
    void initialized() {
        LOGGER.info("SCASP metrics singleton initialized");
    }

    @PreDestroy
    void shuttingDown() {
        LOGGER.info("SCASP metrics singleton is shutting down");
    }

    @Override
    @Lock(LockType.READ)
    public long successfulRegistrations() {
        return registrations.sum();
    }

    @Override
    @Lock(LockType.WRITE)
    public void recordSuccessfulRegistration() {
        registrations.increment();
    }

    @Override
    @Lock(LockType.READ)
    public long timerExecutions() {
        return timerExecutions.sum();
    }

    @Override
    @Lock(LockType.WRITE)
    public void recordTimerExecution() {
        timerExecutions.increment();
    }

    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = false)
    @Lock(LockType.WRITE)
    public void scheduledMetricsRefresh() {
        timerExecutions.increment();
    }
}
