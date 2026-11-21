package ru.example.library.store;

import ru.example.library.model.Book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Простые замеры BST и TreeMap на одинаковых ключах.
 * Результаты ориентировочные: зависят от JVM, оборудования и фоновой нагрузки.
 */
public final class BstBenchmark {
    private static final int[] SIZES = {10_000, 100_000};

    private BstBenchmark() { }

    public static String run() {
        StringBuilder report = new StringBuilder();
        report.append("Замеры времени, миллисекунды\n")
              .append("Ключи перемешаны; поиск — успешный; перед каждым типом операции структура создаётся заново.\n\n")
              .append(String.format(Locale.ROOT, "%-10s | %-12s | %-12s | %-12s%n",
                      "Размер", "Операция", "BST", "TreeMap"));

        for (int size : SIZES) {
            List<String> keys = createKeys(size);
            report.append(String.format(Locale.ROOT, "%-10d | %-12s | %-12s | %-12s%n",
                    size, "Вставка", millis(insertBst(keys)), millis(insertTreeMap(keys))));
            report.append(String.format(Locale.ROOT, "%-10d | %-12s | %-12s | %-12s%n",
                    size, "Поиск", millis(searchBst(keys)), millis(searchTreeMap(keys))));
            report.append(String.format(Locale.ROOT, "%-10d | %-12s | %-12s | %-12s%n",
                    size, "Удаление", millis(deleteBst(keys)), millis(deleteTreeMap(keys))));
        }
        report.append("\nВремя включает только измеряемую операцию; подготовка данных не учитывается.");
        return report.toString();
    }

    private static List<String> createKeys(int size) {
        List<String> keys = new ArrayList<>(size);
        for (int i = 0; i < size; i++) keys.add(String.format(Locale.ROOT, "Book-%06d", i));
        Collections.shuffle(keys, new java.util.Random(42));
        return keys;
    }

    private static List<Book> books(List<String> keys) {
        List<Book> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            result.add(new Book("ISBN-" + key, key, "Benchmark", 2020, "Test", false));
        }
        return result;
    }

    private static long insertBst(List<String> keys) {
        BstBookStore store = new BstBookStore();
        List<Book> data = books(keys);
        long start = System.nanoTime();
        for (Book book : data) store.add(book);
        return System.nanoTime() - start;
    }

    private static long insertTreeMap(List<String> keys) {
        Map<String, Book> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<Book> data = books(keys);
        long start = System.nanoTime();
        for (Book book : data) map.put(book.getTitle(), book);
        return System.nanoTime() - start;
    }

    private static long searchBst(List<String> keys) {
        BstBookStore store = new BstBookStore();
        for (Book book : books(keys)) store.add(book);
        long start = System.nanoTime();
        for (String key : keys) store.findByTitle(key);
        return System.nanoTime() - start;
    }

    private static long searchTreeMap(List<String> keys) {
        Map<String, Book> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Book book : books(keys)) map.put(book.getTitle(), book);
        long start = System.nanoTime();
        for (String key : keys) map.get(key);
        return System.nanoTime() - start;
    }

    private static long deleteBst(List<String> keys) {
        BstBookStore store = new BstBookStore();
        for (Book book : books(keys)) store.add(book);
        long start = System.nanoTime();
        for (String key : keys) store.removeByTitle(key);
        return System.nanoTime() - start;
    }

    private static long deleteTreeMap(List<String> keys) {
        Map<String, Book> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Book book : books(keys)) map.put(book.getTitle(), book);
        long start = System.nanoTime();
        for (String key : keys) map.remove(key);
        return System.nanoTime() - start;
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }
}
