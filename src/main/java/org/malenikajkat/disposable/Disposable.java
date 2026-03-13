package org.malenikajkat.disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Disposable {
    void dispose();
    boolean isDisposed();

    default void doOnDispose(Runnable action) {
        if (this instanceof DisposableWithOnDispose) {
            ((DisposableWithOnDispose) this).addOnDispose(action);
        }
    }

    interface DisposableWithOnDispose extends Disposable {
        void addOnDispose(Runnable action);
    }

    class Composite implements Disposable, DisposableWithOnDispose {
        private final List<Runnable> onDisposeActions = new ArrayList<>();
        private final List<Disposable> disposables = new ArrayList<>();
        private final Object lock = new Object();
        private final AtomicBoolean disposed = new AtomicBoolean(false);

        public void add(Disposable d) {
            synchronized (lock) {
                if (!disposed.get()) {
                    disposables.add(d);
                } else {
                    d.dispose();
                }
            }
        }

        @Override
        public void addOnDispose(Runnable action) {
            synchronized (lock) {
                if (!disposed.get()) {
                    onDisposeActions.add(action);
                } else {
                    action.run();
                }
            }
        }

        @Override
        public void dispose() {
            if (disposed.compareAndSet(false, true)) {
                synchronized (lock) {
                    onDisposeActions.forEach(Runnable::run);
                    onDisposeActions.clear();
                    for (Disposable d : disposables) {
                        d.dispose();
                    }
                    disposables.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}