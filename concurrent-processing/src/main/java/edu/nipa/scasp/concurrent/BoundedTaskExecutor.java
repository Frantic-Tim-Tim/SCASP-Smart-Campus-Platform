package edu.nipa.scasp.concurrent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BoundedTaskExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final Semaphore capacity;
    private final Duration admissionTimeout;
    private final Duration shutdownTimeout;
    private final TaskStatistics statistics = new TaskStatistics();

    public BoundedTaskExecutor(int workers, int queueCapacity, Duration admissionTimeout,
                               Duration shutdownTimeout, ThreadFactory threadFactory) {
        if (workers < 1 || queueCapacity < 1) {
            throw new IllegalArgumentException("workers and queueCapacity must be positive");
        }
        this.admissionTimeout = requirePositive(admissionTimeout, "admissionTimeout");
        this.shutdownTimeout = requirePositive(shutdownTimeout, "shutdownTimeout");
        capacity = new Semaphore(workers + queueCapacity, true);
        executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), Objects.requireNonNull(threadFactory,
                "threadFactory"), new ThreadPoolExecutor.AbortPolicy());
    }

    public <T> Future<T> submit(Callable<T> task) throws InterruptedException {
        Objects.requireNonNull(task, "task");
        if (!capacity.tryAcquire(admissionTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
            statistics.rejection();
            throw new RejectedExecutionException("Task admission timed out");
        }
        try {
            AtomicBoolean released = new AtomicBoolean();
            FutureTask<T> future = new FutureTask<>(() -> {
                long started = System.nanoTime();
                try {
                    T result = task.call();
                    statistics.success(System.nanoTime() - started);
                    return result;
                } catch (Exception | Error failure) {
                    statistics.failure(System.nanoTime() - started);
                    throw failure;
                } finally {
                    releasePermit(released);
                }
            }) {
                @Override
                protected void done() {
                    if (isCancelled()) {
                        executor.remove(this);
                        releasePermit(released);
                    }
                }
            };
            executor.execute(future);
            return future;
        } catch (RejectedExecutionException failure) {
            capacity.release();
            statistics.rejection();
            throw failure;
        }
    }

    public Future<?> submit(Runnable task) throws InterruptedException {
        Objects.requireNonNull(task, "task");
        return submit(() -> {
            task.run();
            return null;
        });
    }

    public TaskStatistics.Snapshot statistics() {
        return statistics.snapshot();
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(shutdownTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
                    throw new IllegalStateException("Worker threads did not terminate");
                }
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    private void releasePermit(AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            capacity.release();
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
