package edu.nipa.scasp.concurrent;

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

public final class TaskStatistics {
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final LongAdder processingNanos = new LongAdder();

    void success(long elapsedNanos) {
        succeeded.increment();
        processingNanos.add(elapsedNanos);
    }

    void failure(long elapsedNanos) {
        failed.increment();
        processingNanos.add(elapsedNanos);
    }

    void rejection() {
        rejected.increment();
    }

    public Snapshot snapshot() {
        return new Snapshot(succeeded.sum(), failed.sum(), rejected.sum(),
                Duration.ofNanos(processingNanos.sum()));
    }

    public record Snapshot(long succeeded, long failed, long rejected,
                           Duration totalProcessingTime) {
    }
}
