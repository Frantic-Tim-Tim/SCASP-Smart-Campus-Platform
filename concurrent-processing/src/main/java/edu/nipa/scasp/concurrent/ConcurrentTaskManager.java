package edu.nipa.scasp.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ConcurrentTaskManager implements AutoCloseable {

    private final ThreadPoolExecutor executor;

    public ConcurrentTaskManager(int numberOfWorkers, int queueCapacity) {
        if (numberOfWorkers < 1) {
            throw new IllegalArgumentException(
                    "numberOfWorkers must be at least 1"
            );
        }

        if (queueCapacity < 1) {
            throw new IllegalArgumentException(
                    "queueCapacity must be at least 1"
            );
        }

        this.executor = new ThreadPoolExecutor(
                numberOfWorkers,
                numberOfWorkers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public Future<TaskResult> submit(CampusTask task) {
        Objects.requireNonNull(task, "task");
        return submit((Callable<TaskResult>) task);
    }

    public Future<TaskResult> submit(Callable<TaskResult> task) {
        Objects.requireNonNull(task, "task");
        return executor.submit(task);
    }

    public Future<?> submit(Runnable task) {
    	Objects.requireNonNull(task, "task");
    	return executor.submit(task);
    }

    public long getCompletedTaskCount() {
    	return executor.getCompletedTaskCount();
    }

    public int getPoolSize() {
    	return executor.getPoolSize();
    }
    public List<Future<TaskResult>> submitAll(List<CampusTask> tasks) {
        Objects.requireNonNull(tasks, "tasks");

        return tasks.stream()
                .map(this::submit)
                .toList();
    }

    public int getActiveWorkerCount() {
        return executor.getActiveCount();
    }

    public int getPendingTaskCount() {
        return executor.getQueue().size();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void close() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
