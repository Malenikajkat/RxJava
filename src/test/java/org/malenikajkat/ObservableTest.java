package org.malenikajkat;

import org.junit.jupiter.api.Test;
import org.malenikajkat.disposable.Disposable;
import org.malenikajkat.observable.Observable;
import org.malenikajkat.observer.Observer;
import org.malenikajkat.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableTest {

    @Test
    public void testJustAndMapFilter() throws Exception {
        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Disposable disposable = Observable.just(1, 2, 3, 4)
                .map(x -> x * 2)
                .filter(x -> x > 4)
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { results.add(item); }
                    @Override public void onError(Throwable throwable) { throwable.printStackTrace(); latch.countDown(); }
                    @Override public void onComplete() { latch.countDown(); }
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        disposable.dispose();
        assertEquals(List.of(6, 8), results);
    }

    @Test
    public void testSubscribeOnIo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String[] threadName = {null};

        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {
                        threadName[0] = Thread.currentThread().getName();
                        latch.countDown();
                    }
                    @Override public void onError(Throwable throwable) { throwable.printStackTrace(); latch.countDown(); }
                    @Override public void onComplete() {}
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(threadName[0].startsWith("IO-thread"));
    }

    @Test
    public void testObserveOnComputation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String[] threadName = {null};

        Observable.just(1)
                .observeOn(Schedulers.computation())
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {
                        threadName[0] = Thread.currentThread().getName();
                        latch.countDown();
                    }
                    @Override public void onError(Throwable throwable) { throwable.printStackTrace(); latch.countDown(); }
                    @Override public void onComplete() {}
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(threadName[0].startsWith("Compute-thread"));
    }

    @Test
    public void testFlatMap() throws Exception {
        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2)
                .flatMap(x -> Observable.just(x + 10, x + 20))
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { results.add(item); }
                    @Override public void onError(Throwable throwable) { throwable.printStackTrace(); latch.countDown(); }
                    @Override public void onComplete() { latch.countDown(); }
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of(11, 21, 12, 22), results);
    }

    @Test
    public void testErrorHandling() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = {null};

        Observable.<Integer>create((d, obs) -> {
            obs.onError(new RuntimeException("Test error"));
        }).subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) {}
            @Override public void onError(Throwable err) { error[0] = err; latch.countDown(); }
            @Override public void onComplete() { latch.countDown(); }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(error[0]);
        assertEquals("Test error", error[0].getMessage());
    }
}