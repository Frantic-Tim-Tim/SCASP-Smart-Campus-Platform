
package edu.nipa.scasp.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskCounter {

    // Shared counter protected by synchronized methods.
    private int synchronizedCount = 0;

    // Thread-safe counter using AtomicInteger.
    private final AtomicInteger atomicCount = new AtomicInteger(0);

    public synchronized void incrementSynchronized() {
        synchronizedCount++;
    }

    public int getSynchronizedCount() {
        return synchronizedCount;
    }

    public void incrementAtomic() {
        atomicCount.incrementAndGet();
    }

    public int getAtomicCount() {
        return atomicCount.get();
    }
}
