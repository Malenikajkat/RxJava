package org.malenikajkat.schedulers;

public class Schedulers {
    private static final Scheduler IO = new IOThreadScheduler();
    private static final Scheduler COMPUTATION = new ComputationScheduler();

    public static Scheduler io() {
        return IO;
    }

    public static Scheduler computation() {
        return COMPUTATION;
    }

    public static Scheduler single() {
        return new SingleThreadScheduler();
    }
}