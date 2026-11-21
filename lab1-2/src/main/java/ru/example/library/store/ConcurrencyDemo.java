package ru.example.library.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Console demo of a data race and protection of concurrent BST access.
 *
 * Run: ru.example.library.store.ConcurrencyDemo
 */
public final class ConcurrencyDemo {
    private static final int THREAD_COUNT = 4;
    private static final int VALUES_PER_THREAD = 1_000;

    private ConcurrencyDemo() {
    }

    public static void main(String[] args) {
        int unsafeSize = demonstrateLostNode();
        System.out.println("Unsafe BST: expected 3 nodes, got " + unsafeSize);

        BstStore<Integer, Integer> smallSafeTree = new BstStore<>(value -> value);
        smallSafeTree.add(10);
        runTwoThreads(
                () -> smallSafeTree.add(5),
                () -> smallSafeTree.add(7)
        );
        System.out.println("Synchronized BST: expected 3 nodes, got "
                + smallSafeTree.size());

        int expectedSize = THREAD_COUNT * VALUES_PER_THREAD;
        BstStore<Integer, Integer> stressTree = new BstStore<>(value -> value);
        writeConcurrently(stressTree);
        System.out.println("Concurrent insertion into BstStore: expected " + expectedSize
                + " nodes, got " + stressTree.size());

        if (unsafeSize != 2 || smallSafeTree.size() != 3
                || stressTree.size() != expectedSize) {
            throw new IllegalStateException("Demo results do not match expectations");
        }
        System.out.println("Demo completed successfully.");
    }

    /**
     * Two threads insert different values into the empty left subtree simultaneously.
     * Both read null before either writes, so the last write overwrites the first one.
     */
    private static int demonstrateLostNode() {
        UnsafeIntBst tree = new UnsafeIntBst();
        tree.addRoot(10);

        CountDownLatch bothThreadsRead = new CountDownLatch(2);
        CountDownLatch allowWrites = new CountDownLatch(1);
        Thread first = new Thread(
                () -> tree.insertLeftOfRoot(5, bothThreadsRead, allowWrites),
                "unsafe-bst-writer-1"
        );
        Thread second = new Thread(
                () -> tree.insertLeftOfRoot(7, bothThreadsRead, allowWrites),
                "unsafe-bst-writer-2"
        );

        first.start();
        second.start();
        await(bothThreadsRead);
        allowWrites.countDown();
        join(first);
        join(second);
        return tree.size();
    }

    private static void runTwoThreads(Runnable firstOperation, Runnable secondOperation) {
        Thread first = new Thread(firstOperation, "safe-bst-writer-1");
        Thread second = new Thread(secondOperation, "safe-bst-writer-2");
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
                try {
                    await(start);
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
            throw new IllegalStateException("Concurrent insertion failed", failure.get());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted", exception);
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a thread", exception);
        }
    }

    /**
     * Minimal intentionally unprotected BST used only to demonstrate a data race.
     * The synchronized BstStore is used in production code instead.
     */
    private static final class UnsafeIntBst {
        private static final class Node {
            private final int value;
            private Node left;
            private Node right;

            private Node(int value) {
                this.value = value;
            }
        }

        private Node root;

        private void addRoot(int value) {
            root = new Node(value);
        }

        private void insertLeftOfRoot(int value, CountDownLatch bothThreadsRead,
                                      CountDownLatch allowWrites) {
            Node observedChild = root.left;
            bothThreadsRead.countDown();
            await(allowWrites);
            if (observedChild == null) {
                root.left = new Node(value);
            }
        }

        private int size() {
            return count(root);
        }

        private int count(Node node) {
            if (node == null) return 0;
            return 1 + count(node.left) + count(node.right);
        }
    }
}
