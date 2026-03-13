package org.malenikajkat.operators;

import org.malenikajkat.observable.Observable;
import org.malenikajkat.observer.Observer;

public interface Operator<T, R> {
    void apply(Observable<T> upstream, Observer<? super R> downstream);
}