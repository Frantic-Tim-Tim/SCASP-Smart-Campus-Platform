package edu.nipa.scasp.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class RaceConditionDemonstration {
    public Result run() {
        int unsafeResult = runLostUpdate();
        int correctedResult = runAtomicIncrement();
        return new Result(unsafeResult, correctedResult);
    }

    private static int runLostUpdate() {
        int[] counter = {0};
        CyclicBarrier bothRead = new CyclicBarrier(2);
        List<Callable<Void>> tasks = List.of(
                () -> unsafeIncrement(counter, bothRead),
                () -> unsafeIncrement(counter, bothRead));
        runTasks(tasks);
        return counter[0];
    }

    private static Void unsafeIncrement(int[] counter, CyclicBarrier bothRead)
            throws Exception {
        int observed = counter[0];
        bothRead.await();
        counter[0] = observed + 1;
        return null;
    }

    private static int runAtomicIncrement() {
        AtomicInteger counter = new AtomicInteger();
        CyclicBarrier bothReady = new CyclicBarrier(2);
        List<Callable<Void>> tasks = List.of(() -> {
            bothReady.await();
            counter.incrementAndGet();
            return null;
        }, () -> {
            bothReady.await();
            counter.incrementAndGet();
            return null;
        });
        runTasks(tasks);
        return counter.get();
    }

    private static void runTasks(List<Callable<Void>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (Future<Void> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Race demonstration was interrupted", interrupted);
        } catch (ExecutionException failure) {
            throw new IllegalStateException("Race demonstration task failed", failure.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    public record Result(int unsafeValue, int correctedValue) {
    }
}
