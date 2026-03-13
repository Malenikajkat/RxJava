# Реализация RxJava

> 🚀 Реактивное программирование на чистом Java — без зависимостей, с полным контролем над потоками, подписками и операторами.

Ссылка на репозиторий: [https://github.com/Malenikajkat/RxJava](https://github.com/Malenikajkat/RxJava)

---

## 🎯 Цель работы

Создать собственную версию библиотеки RxJava, используя основные концепции реактивного программирования:
- Паттерн «Наблюдатель» (Observer)
- Управление жизненным циклом через `Disposable`
- Асинхронное выполнение с `Schedulers`
- Операторы преобразования: `map`, `filter`, `flatMap`

Цель — продемонстрировать понимание внутреннего устройства реактивных потоков и реализовать систему, полностью соответствующую требованиям.

---

## 🔧 Блок 1: Реализация базовых компонентов

### ✅ Интерфейс `Observer<T>`
```java src/main/java/org/malenikajkat/observer/Observer.java
public interface Observer<T> {
    void onNext(T item);
    void onError(Throwable error);
    void onComplete();
}
```
- Получает элементы потока (`onNext`)
- Обрабатывает ошибки (`onError`)
- Сигнализирует о завершении (`onComplete`)

### ✅ Класс `Observable<T>`
Реализует реактивный источник данных:
- Поддержка подписки: `subscribe(Observer)`
- Создание потоков: `create(OnSubscribe)`, `just(T...)`
- Полная изоляция логики эмиссии от подписчика

### ✅ Пример создания потока
```java src/main/java/org/malenikajkat/Main.java
Observable.create((disposable, observer) -> {
    for (int i = 1; i <= 3; i++) {
        if (disposable.isDisposed()) return;
        observer.onNext(i);
    }
    observer.onComplete();
});
```

---

## 🔁 Блок 2: Операторы преобразования данных

Реализованы ключевые операторы:

| Оператор | Назначение |
|--------|-----------|
| `map(Function<T,R>)` | Преобразует каждый элемент: `x → x * 2` |
| `filter(Predicate<T>)` | Фильтрует по условию: `x > 10` |
| `flatMap(Function<T, Observable<R>>)` | Преобразует элемент в новый поток и "разворачивает" его |

### Пример цепочки
```java src/main/java/org/malenikajkat/Main.java
Observable.just(1, 2, 3)
    .map(x -> x * 10)           // → 10, 20, 30
    .filter(x -> x > 15)        // → 20, 30
    .flatMap(x -> Observable.just(x + 1, x + 2))  // → 21, 22, 31, 32
    .subscribe(System.out::println);
```

---

## ⚙️ Блок 3: Управление потоками выполнения

### ✅ Интерфейс `Scheduler`
```java src/main/java/org/malenikajkat/schedulers/Scheduler.java
public interface Scheduler {
    Worker createWorker();

    interface Worker {
        void schedule(Runnable task);
        void dispose();
        boolean isDisposed();
    }
}
```

### ✅ Три реализации планировщиков

| Планировщик | Назначение | Пул потоков |
|------------|----------|-------------|
| `IOThreadScheduler` | Блокирующие операции (сеть, файлы) | `CachedThreadPool` |
| `ComputationScheduler` | CPU-интенсивные задачи | `FixedThreadPool` (cores) |
| `SingleThreadScheduler` | Последовательные операции | `SingleThreadExecutor` |

Все потоки — **демоны**, JVM завершается автоматически.

### ✅ Методы управления потоками
- `subscribeOn(Scheduler)` — подписка происходит в указанном потоке
- `observeOn(Scheduler)` — обработка событий (`onNext`, `onError`, `onComplete`) переключается на другой поток

#### Пример
```java src/main/java/org/malenikajkat/Main.java
observable
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.computation())
    .map(String::valueOf)
    .subscribe(System.out::println);
```

---

## 🛠️ Блок 4: Дополнительные операторы и управление подписками

### ✅ `Disposable` — отмена подписки
Интерфейс:
```java src/main/java/org/malenikajkat/disposable/Disposable.java
interface Disposable {
    void dispose();
    boolean isDisposed();
}
```

Поддержка:
- Отмены подписки
- Колбэков при отмене: `doOnDispose(Runnable)`
- Композиции: `Disposable.Composite` — объединение нескольких подписок

### ✅ `flatMap` — корректная работа с внутренними потоками
- Каждый внутренний `Observable` управляется отдельно
- При отмене внешней подписки — все внутренние также отменяются
- Используется `CompositeDisposable` для сборки ресурсов

### ✅ Обработка ошибок
- Все исключения перехватываются и передаются в `onError`
- В `map` используется `try-catch`
- При `onError` подписка завершается, ресурсы освобождаются

---

## 🧪 Блок 5: Тестирование

### ✅ Юнит-тесты (JUnit 5)

Файл: `src/test/java/org/malenikajkat/ObservableTest.java`

Проверены:
- `map`, `filter`, `flatMap`
- `subscribeOn`, `observeOn`
- Обработка ошибок
- Работа в многопоточной среде
- Отмена подписки

### Пример теста
```java src/test/java/org/malenikajkat/ObservableTest.java
@Test
void testFlatMap() throws Exception {
    List<Integer> results = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);

    Observable.just(1, 2)
        .flatMap(x -> Observable.just(x + 10, x + 20))
        .subscribe(results::add, err -> {}, latch::countDown);

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertEquals(List.of(11, 21, 12, 22), results);
}
```

Запуск:
```bash

mvn test
```

---

## 📄 Блок 6: Отчёт

### Архитектура системы

```
+------------------+
|   Observable<T>  | ← создание, операторы
+------------------+
         ↓
+------------------+
|    Observer<T>   | ← приём событий
+------------------+
         ↓
+------------------+
|   Disposable     | ← управление жизнью
+------------------+
         ↓
+------------------+
|    Scheduler     | ← многопоточность
+------------------+
```

### Принципы работы Schedulers

| Scheduler | Когда использовать |
|----------|--------------------|
| `io()` | Сеть, файлы, БД — блокирующие операции |
| `computation()` | Расчёты, обработка данных — CPU-bound |
| `single()` | Логгирование, UI-обновления — последовательность важна |

### Процесс тестирования

- Все тесты используют `CountDownLatch` для ожидания асинхронных событий
- Проверяется не только результат, но и имя потока (`Thread.currentThread().getName()`)
- Обрабатываются таймауты и принудительная отмена

---

## 📌 Пример использования

```java src/main/java/org/malenikajkat/Main.java
Disposable disposable = Observable.<Integer>create((d, obs) -> {
        Thread emitter = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                if (d.isDisposed()) return;
                obs.onNext(i);
                Thread.sleep(300);
            }
            obs.onComplete();
        });
        d.doOnDispose(emitter::interrupt);
        emitter.start();
    })
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.computation())
    .map(x -> x * 10)
    .filter(x -> x > 20)
    .flatMap(x -> Observable.just(x + 1, x + 2))
    .subscribe(
        item -> System.out.println("🟢 " + item),
        err -> System.err.println("💥 " + err),
        () -> System.out.println("🎉 Завершено")
    );

