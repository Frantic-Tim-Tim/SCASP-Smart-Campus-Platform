package edu.nipa.scasp.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentTaskManagerTest {

    @Test
    void shouldProcessCampusTaskAndReturnResult() throws Exception {
        try (ConcurrentTaskManager manager =
                     new ConcurrentTaskManager(2, 5)) {

            CampusTask task = new CampusTask(
                    UUID.randomUUID(),
                    TaskType.REGISTRATION,
                    "Register student for Java",
                    Instant.now()
            );

            Future<TaskResult> future = manager.submit(task);

            TaskResult result = future.get(2, TimeUnit.SECONDS);

            assertNotNull(result);
            assertEquals(TaskType.REGISTRATION, result.taskType());
            assertTrue(result.message().contains("Successfully processed"));
        }
    }

    @Test
    void shouldExecuteTasksConcurrentlyAndRejectExcessTasks()
            throws Exception {

        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try (ConcurrentTaskManager manager =
                     new ConcurrentTaskManager(2, 1)) {

            try {
                // Submit two tasks that occupy both worker threads.
                Future<TaskResult> first = manager.submit(
                        blockingTask(started, release, "First task")
                );

                Future<TaskResult> second = manager.submit(
                        blockingTask(started, release, "Second task")
                );

                // Wait until both tasks have started.
                assertTrue(started.await(2, TimeUnit.SECONDS));

                // Both workers are occupied, so this task enters the queue.
                Future<TaskResult> third = manager.submit(
                        () -> createResult("Third task")
                );

                assertEquals(2, manager.getActiveWorkerCount());
                assertEquals(1, manager.getPendingTaskCount());

                // Two active workers + one queued task = capacity reached.
                assertThrows(
                        RejectedExecutionException.class,
                        () -> manager.submit(
                                () -> createResult("Fourth task")
                        )
                );

                // Release the blocked tasks.
                release.countDown();

                assertNotNull(first.get(2, TimeUnit.SECONDS));
                assertNotNull(second.get(2, TimeUnit.SECONDS));
                assertNotNull(third.get(2, TimeUnit.SECONDS));

            } finally {
                // Prevent worker threads from remaining blocked if a test fails.
                release.countDown();
            }
        }
    }

    @Test
    void shouldRejectInvalidWorkerOrQueueConfiguration() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConcurrentTaskManager(0, 5)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConcurrentTaskManager(2, 0)
        );
    }

    private Callable<TaskResult> blockingTask(
            CountDownLatch started,
            CountDownLatch release,
            String description
    ) {
        return () -> {
            started.countDown();
            release.await();
            return createResult(description);
        };
    }

    private TaskResult createResult(String description) {
        return new TaskResult(
                UUID.randomUUID(),
                TaskType.REPORT,
                "Completed: " + description,
                Instant.now()
        );
    }

    @Test
	void shouldExecuteRunnableTask() throws Exception {
   	try (ConcurrentTaskManager manager =
            new ConcurrentTaskManager(2, 5)) {

        CountDownLatch completed = new CountDownLatch(1);

        Future<?> future = manager.submit(
                (Runnable) completed::countDown
        );

        future.get(2, TimeUnit.SECONDS);

        assertEquals(0, completed.getCount());
    	}
    }


    @Test
    void shouldPreventRaceConditionsUsingSynchronizedAndAtomicInteger()
            throws Exception {

        TaskCounter counter = new TaskCounter();

        int numberOfThreads = 10;
        int incrementsPerThread = 10_000;

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(numberOfThreads)) {

            for (int i = 0; i < numberOfThreads; i++) {

                executor.submit(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.incrementSynchronized();
                        counter.incrementAtomic();
                    }
                });
            }

            executor.shutdown();

            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        int expectedCount = numberOfThreads * incrementsPerThread;

        assertEquals(expectedCount, counter.getSynchronizedCount());
        assertEquals(expectedCount, counter.getAtomicCount());
    }

}
