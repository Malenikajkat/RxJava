package org.malenikajkat.schedulers;

public interface Scheduler {
    Worker createWorker();

    interface Worker {
        void schedule(Runnable task);
        void dispose();
        boolean isDisposed();
    }
}