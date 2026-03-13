package org.malenikajkat.operators;

import org.malenikajkat.disposable.Disposable;
import org.malenikajkat.observable.Observable;
import org.malenikajkat.observer.Observer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class FlatMapOperator<T, R> implements Operator<T, R> {
    private final Function<? super T, ? extends Observable<? extends R>> mapper;

    public FlatMapOperator(Function<? super T, ? extends Observable<? extends R>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void apply(Observable<T> upstream, Observer<? super R> downstream) {
        Disposable.Composite composite = new Disposable.Composite();
        AtomicBoolean terminated = new AtomicBoolean(false);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (terminated.get()) return;

                Observable<? extends R> inner = mapper.apply(item);
                Disposable innerDisposable = inner.subscribe(
                    downstream::onNext,
                    error -> {
                        if (terminated.compareAndSet(false, true)) {
                            composite.dispose();
                            downstream.onError(error);
                        }
                    },
                    () -> {}
                );
                composite.add(innerDisposable);
            }

            @Override
            public void onError(Throwable throwable) {
                if (terminated.compareAndSet(false, true)) {
                    composite.dispose();
                    downstream.onError(throwable);
                }
            }

            @Override
            public void onComplete() {
                if (terminated.compareAndSet(false, true)) {
                    downstream.onComplete();
                    composite.dispose();
                }
            }
        };

        Disposable upstreamDisposable = upstream.subscribe(observer);
        composite.add(upstreamDisposable);
    }
}