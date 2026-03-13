package org.malenikajkat.operators;

import org.malenikajkat.observable.Observable;
import org.malenikajkat.observer.Observer;

import java.util.function.Predicate;

public class FilterOperator<T> implements Operator<T, T> {
    private final Predicate<? super T> predicate;

    public FilterOperator(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void apply(Observable<T> upstream, Observer<? super T> downstream) {
        upstream.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (predicate.test(item)) {
                    downstream.onNext(item);
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