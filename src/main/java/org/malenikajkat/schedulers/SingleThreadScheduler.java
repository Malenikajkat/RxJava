package org.malenikajkat.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SingleThreadScheduler implements Scheduler {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Single-thread");
            return t;
        }
    });

    @Override
    public Worker createWorker() {
        return new Worker() {
            private volatile boolean disposed = false;

            @Override
            public void dispose() {
                disposed = true;
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }

            @Override
            public void schedule(Runnable task) {
                if (!isDisposed()) {
                    executor.submit(task);
                }
            }
        };
    }
}