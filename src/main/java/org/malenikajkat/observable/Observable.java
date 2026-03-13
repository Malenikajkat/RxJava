package org.malenikajkat.observable;

import org.malenikajkat.disposable.Disposable;
import org.malenikajkat.observer.Observer;
import org.malenikajkat.operators.FlatMapOperator;
import org.malenikajkat.operators.FilterOperator;
import org.malenikajkat.operators.MapOperator;
import org.malenikajkat.schedulers.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Observable<T> {

    private final OnSubscribe<T> source;

    private Observable(OnSubscribe<T> source) {
        this.source = source;
    }

    public static <T> Observable<T> create(OnSubscribe<T> source) {
        return new Observable<>(source);
    }

    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create((disposable, observer) -> {
            for (T item : items) {
                if (disposable.isDisposed()) return;
                observer.onNext(item);
            }
            observer.onComplete();
        });
    }

    @FunctionalInterface
    public interface OnSubscribe<T> {
        void subscribe(Disposable disposable, Observer<? super T> observer);
    }

    public Disposable subscribe(Observer<? super T> observer) {
        Disposable.Composite compositeDisposable = new Disposable.Composite();
        SafeObserver<T> safeObserver = new SafeObserver<>(observer, compositeDisposable);
        BooleanDisposable sourceDisposable = new BooleanDisposable();
        compositeDisposable.add(sourceDisposable);
        source.subscribe(sourceDisposable, safeObserver);
        return compositeDisposable;
    }

    public Disposable subscribe(
            Consumer<? super T> onNext,
            Consumer<Throwable> onError,
            Runnable onComplete) {

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onNext(T item) {
                onNext.accept(item);
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        };

        return subscribe(observer);
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(new OnSubscribe<T>() {
            @Override
            public void subscribe(Disposable disposable, Observer<? super T> observer) {
                Scheduler.Worker worker = scheduler.createWorker();
                disposable.doOnDispose(worker::dispose);

                worker.schedule(() -> {
                    if (!disposable.isDisposed()) {
                        source.subscribe(disposable, observer);
                    }
                });
            }
        });
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(new OnSubscribe<T>() {
            @Override
            public void subscribe(Disposable disposable, Observer<? super T> downstreamObserver) {
                Scheduler.Worker worker = scheduler.createWorker();
                disposable.doOnDispose(worker::dispose);

                source.subscribe(disposable, new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        worker.schedule(() -> {
                            if (!disposable.isDisposed()) {
                                downstreamObserver.onNext(item);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        worker.schedule(() -> {
                            if (!disposable.isDisposed()) {
                                downstreamObserver.onError(throwable);
                            }
                        });
                    }

                    @Override
                    public void onComplete() {
                        worker.schedule(() -> {
                            if (!disposable.isDisposed()) {
                                downstreamObserver.onComplete();
                            }
                            worker.dispose();
                        });
                    }
                });
            }
        });
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new MapOperator<>(mapper));
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return lift(new FilterOperator<>(predicate));
    }

    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return lift(new FlatMapOperator<>(mapper));
    }

    private <R> Observable<R> lift(org.malenikajkat.operators.Operator<T, R> operator) {
        return new Observable<>(new OnSubscribe<R>() {
            @Override
            public void subscribe(Disposable disposable, Observer<? super R> downstreamObserver) {
                operator.apply(Observable.this, downstreamObserver);
            }
        });
    }

    private static class SafeObserver<T> implements Observer<T> {
        private final Observer<? super T> downstream;
        private final Disposable disposable;
        private volatile boolean done = false;

        public SafeObserver(Observer<? super T> downstream, Disposable disposable) {
            this.downstream = downstream;
            this.disposable = disposable;
        }

        @Override
        public void onNext(T item) {
            if (done || disposable.isDisposed()) return;
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            if (done) return;
            done = true;
            downstream.onError(throwable);
            disposable.dispose();
        }

        @Override
        public void onComplete() {
            if (done) return;
            done = true;
            downstream.onComplete();
            disposable.dispose();
        }
    }

    public static class BooleanDisposable implements Disposable, Disposable.DisposableWithOnDispose {
        private final AtomicBoolean disposed = new AtomicBoolean(false);
        private volatile Runnable onDisposeAction;

        @Override
        public void addOnDispose(Runnable action) {
            if (isDisposed()) {
                action.run();
            } else {
                synchronized (this) {
                    if (!disposed.get()) {
                        onDisposeAction = action;
                    } else {
                        action.run();
                    }
                }
            }
        }

        @Override
        public void dispose() {
            if (disposed.compareAndSet(false, true)) {
                if (onDisposeAction != null) {
                    onDisposeAction.run();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}