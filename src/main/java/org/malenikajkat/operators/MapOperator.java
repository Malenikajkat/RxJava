package org.malenikajkat.operators;

import org.malenikajkat.observable.Observable;
import org.malenikajkat.observer.Observer;

import java.util.function.Function;

public class MapOperator<T, R> implements Operator<T, R> {
    private final Function<? super T, ? extends R> mapper;

    public MapOperator(Function<? super T, ? extends R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void apply(Observable<T> upstream, Observer<? super R> downstream) {
        upstream.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                try {
                    downstream.onNext(mapper.apply(item));
                } catch (Throwable e) {
                    downstream.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                downstream.onError(throwable);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        });
    }
}