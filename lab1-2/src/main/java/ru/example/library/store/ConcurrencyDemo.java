package ru.example.library.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Консольная демонстрация гонки данных и защиты конкурентного доступа.
 *
 * Запуск: ru.example.library.store.ConcurrencyDemo
 *
 * Для демонстрации потерянного обновления используется минимальный общий счётчик:
 * оба потока читают одно значение до того, как любой из них выполнит запись.
 * Затем отдельно проверяется конкурентная запись в настоящее BstStore.
 */
public final class ConcurrencyDemo {
    private static final int THREAD_COUNT = 4;
    private static final int VALUES_PER_THREAD = 1_000;

    private ConcurrencyDemo() {
    }

    public static void main(String[] args) {
        int unsafeResult = demonstrateLostUpdate();
        System.out.println("Незащищённый счётчик: ожидалось 2, получено " + unsafeResult);

        SynchronizedCounter safeCounter = new SynchronizedCounter();
        runTwoThreads(safeCounter::increment);
        System.out.println("Синхронизированный счётчик: ожидалось 2, получено "
                + safeCounter.value());

        int expectedSize = THREAD_COUNT * VALUES_PER_THREAD;
        BstStore<Integer, Integer> tree = new BstStore<>(value -> value);
        writeConcurrently(tree);
        System.out.println("BstStore: ожидалось " + expectedSize
                + " узлов, получено " + tree.size());

        if (unsafeResult != 1 || safeCounter.value() != 2 || tree.size() != expectedSize) {
            throw new IllegalStateException("Результаты демонстрации не соответствуют ожиданиям");
        }
        System.out.println("Демонстрация завершена успешно.");
    }

    /**
     * Гарантированно воспроизводит lost update: оба потока считывают ноль,
     * после чего независимо записывают единицу.
     */
    private static int demonstrateLostUpdate() {
        final class UnsafeCounter {
            private int value;
            private final CountDownLatch bothThreadsRead = new CountDownLatch(2);
            private final CountDownLatch allowWrites = new CountDownLatch(1);

            private void increment() {
                int observed = value;
                bothThreadsRead.countDown();
                await(allowWrites);
                value = observed + 1;
            }
        }

        UnsafeCounter counter = new UnsafeCounter();
        Thread first = new Thread(counter::increment, "unsafe-writer-1");
        Thread second = new Thread(counter::increment, "unsafe-writer-2");
        first.start();
        second.start();

        await(counter.bothThreadsRead);
        counter.allowWrites.countDown();
        join(first);
        join(second);
        return counter.value;
    }

    private static void runTwoThreads(Runnable operation) {
        Thread first = new Thread(operation, "safe-writer-1");
        Thread second = new Thread(operation, "safe-writer-2");
        first.start();
        second.start();
        join(first);
        join(second);
    }

    private static void writeConcurrently(BstStore<Integer, Integer> tree) {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[THREAD_COUNT];

        for (int threadIndex = 0; threadIndex < THREAD_COUNT; threadIndex++) {
            final int firstValue = threadIndex * VALUES_PER_THREAD;
            workers[threadIndex] = new Thread(() -> {
                await(start);
                try {
                    for (int offset = 0; offset < VALUES_PER_THREAD; offset++) {
                        int value = firstValue + offset;
                        tree.add(value);
                    }
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            }, "bst-writer-" + threadIndex);
            workers[threadIndex].start();
        }

        start.countDown();
        for (Thread worker : workers) {
            join(worker);
        }

        if (failure.get() != null) {
            throw new IllegalStateException("Ошибка конкурентной вставки", failure.get());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Поток был прерван", exception);
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание потока было прервано", exception);
        }
    }

    private static final class SynchronizedCounter {
        private int value;

        public synchronized void increment() {
            value++;
        }

        public synchronized int value() {
            return value;
        }
    }
}
