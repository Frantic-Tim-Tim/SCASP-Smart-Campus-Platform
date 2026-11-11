package edu.nipa.scasp.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {
    @Test
    void boundedExecutorAppliesBackPressureAndTracksOutcomes() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BoundedTaskExecutor executor = new BoundedTaskExecutor(1, 1, Duration.ofMillis(30),
                Duration.ofSeconds(2), Thread.ofPlatform().factory());
        try {
            executor.submit(() -> {
                started.countDown();
                release.await();
                return "done";
            });
            if (!started.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Worker did not start");
            }
            executor.submit(() -> "queued");
            assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> "rejected"));
            release.countDown();
        } finally {
            release.countDown();
            executor.shutdown();
        }
        assertEquals(2, executor.statistics().succeeded());
        assertEquals(1, executor.statistics().rejected());
    }

    @Test
    void deterministicRaceShowsLostUpdateAndItsAtomicCorrection() {
        RaceConditionDemonstration.Result result = new RaceConditionDemonstration().run();

        assertEquals(1, result.unsafeValue());
        assertEquals(2, result.correctedValue());
    }

    @Test
    void comparesPlatformAndVirtualThreadProcessingModels() throws Exception {
        ThreadModelComparison.Comparison comparison =
                new ThreadModelComparison().compare(4, 1);

        assertEquals(4, comparison.taskCount());
        org.junit.jupiter.api.Assertions.assertTrue(comparison.platformWorkers() >= 1);
        org.junit.jupiter.api.Assertions.assertTrue(comparison.platformWorkers() <= 4);
        org.junit.jupiter.api.Assertions.assertTrue(comparison.platformMillis() >= 0);
        org.junit.jupiter.api.Assertions.assertTrue(comparison.virtualThreadMillis() >= 0);
    }

    @Test
    void cancellingQueuedTaskReleasesItsQueueSlot() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BoundedTaskExecutor executor = new BoundedTaskExecutor(1, 1, Duration.ofMillis(100),
                Duration.ofSeconds(2), Thread.ofPlatform().factory());
        try {
            executor.submit(() -> {
                started.countDown();
                release.await();
                return "running";
            });
            if (!started.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Worker did not start");
            }
            Future<String> cancelled = executor.submit(() -> "cancelled");
            org.junit.jupiter.api.Assertions.assertTrue(cancelled.cancel(false));
            executor.submit(() -> "replacement");
            release.countDown();
        } finally {
            release.countDown();
            executor.shutdown();
        }
        assertEquals(2, executor.statistics().succeeded());
    }

    @Test
    void failedCallableIsRecordedAndPropagatedToItsFuture() throws Exception {
        BoundedTaskExecutor executor = new BoundedTaskExecutor(1, 1, Duration.ofMillis(100),
                Duration.ofSeconds(2), Thread.ofPlatform().factory());
        try {
            Future<String> failed = executor.submit(() -> {
                throw new IllegalStateException("expected failure");
            });
            assertThrows(ExecutionException.class, failed::get);
        } finally {
            executor.shutdown();
        }
        assertEquals(1, executor.statistics().failed());
    }
}
