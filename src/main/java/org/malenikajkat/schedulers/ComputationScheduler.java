package org.malenikajkat.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                private final ThreadGroup group = new ThreadGroup("Computation");
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(group, r, "Compute-thread-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            });

    @Override
    public Worker createWorker() {
        return new Worker() {
            private final AtomicBoolean disposed = new AtomicBoolean(false);

            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }

            @Override
            public void schedule(Runnable task) {
                if (!isDisposed()) {
                    executor.submit(() -> {
                        if (!isDisposed()) {
                            task.run();
                        }
                    });
                }
            }
        };
    }
}