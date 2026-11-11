package edu.nipa.scasp.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ThreadModelComparison {
    public Comparison compare(int taskCount, long simulatedIoMillis)
            throws InterruptedException, ExecutionException {
        if (taskCount < 1 || simulatedIoMillis < 0) {
            throw new IllegalArgumentException("taskCount must be positive and delay non-negative");
        }
        int platformWorkers = Math.min(taskCount,
                Math.max(1, Runtime.getRuntime().availableProcessors()));
        long platformTime = measure(Executors.newFixedThreadPool(platformWorkers),
                taskCount, simulatedIoMillis);
        long virtualTime = measure(Executors.newVirtualThreadPerTaskExecutor(),
                taskCount, simulatedIoMillis);
        return new Comparison(platformWorkers, taskCount, platformTime, virtualTime);
    }

    private static long measure(ExecutorService executor, int taskCount, long delayMillis)
            throws InterruptedException, ExecutionException {
        long started = System.nanoTime();
        try (executor) {
            List<Runnable> tasks = new ArrayList<>(taskCount);
            for (int index = 0; index < taskCount; index++) {
                tasks.add(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(delayMillis);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Comparison task interrupted", interrupted);
                    }
                });
            }
            executor.invokeAll(tasks.stream()
                    .<java.util.concurrent.Callable<Void>>map(task -> () -> {
                        task.run();
                        return null;
                    }).toList());
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    public record Comparison(int platformWorkers, int taskCount, long platformMillis,
                             long virtualThreadMillis) {
    }
}
