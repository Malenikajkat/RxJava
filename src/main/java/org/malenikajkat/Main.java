package org.malenikajkat;

import org.malenikajkat.disposable.Disposable;
import org.malenikajkat.observable.Observable;
import org.malenikajkat.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 Запуск реактивного потока...\n");

        CountDownLatch latch = new CountDownLatch(1);

        Disposable disposable = Observable.<Integer>create((disposable1, observer) -> {
                    Thread emitter = new Thread(() -> {
                        try {
                            System.out.println("🧵 Create запущен в: " + Thread.currentThread().getName());
                            for (int i = 1; i <= 5; i++) {
                                if (disposable1.isDisposed()) {
                                    System.out.println("🛑 Подписка отменена — выход из эмиттера.");
                                    return;
                                }
                                System.out.println("📤 Emitting: " + i + " (в потоке: " + Thread.currentThread().getName() + ")");
                                observer.onNext(i);
                                Thread.sleep(300);
                            }
                            observer.onComplete();
                        } catch (InterruptedException e) {
                            if (!disposable1.isDisposed()) {
                                observer.onError(e);
                            }
                        }
                    }, "Emitter-Thread");

                    disposable1.doOnDispose(emitter::interrupt);
                    emitter.start();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map(x -> {
                    int mapped = x * 10;
                    System.out.println("🔁 MAP: " + x + " → " + mapped + " (в потоке: " + Thread.currentThread().getName() + ")");
                    return mapped;
                })
                .filter(x -> {
                    boolean result = x > 20;
                    System.out.println("🔍 FILTER: " + x + " → " + result + " (в потоке: " + Thread.currentThread().getName() + ")");
                    return result;
                })
                .flatMap(x -> Observable.just(x + 1, x + 2))
                .subscribe(
                        item -> System.out.println("🟢 Получено: " + item + " (в потоке: " + Thread.currentThread().getName() + ")"),
                        error -> {
                            System.err.println("💥 Ошибка: " + error.getMessage());
                            error.printStackTrace();
                            latch.countDown();
                        },
                        () -> {
                            System.out.println("🎉 Поток завершён.");
                            latch.countDown();
                        }
                );

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("⏰ Таймаут ожидания завершения потока.");
            disposable.dispose();
        }

    }
}