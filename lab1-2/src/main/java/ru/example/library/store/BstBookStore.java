package ru.example.library.store;

import ru.example.library.model.Book;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Адаптер универсального BST для предметной области электронной библиотеки.
 * Ключ — название книги без учёта регистра и внешних пробелов.
 */
public final class BstBookStore implements BookStore {
    private final BstStore<String, Book> tree =
            new BstStore<>(book -> normalize(book.getTitle()));

    @Override
    public void add(Book book) {
        Objects.requireNonNull(book, "book");
        String title = normalize(book.getTitle());
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Название книги не может быть пустым");
        }
        tree.add(book);
    }

    @Override
    public Book findByTitle(String title) {
        return tree.findByKey(normalize(title));
    }

    @Override
    public boolean removeByTitle(String title) {
        return tree.removeByKey(normalize(title));
    }

    @Override
    public int size() {
        return tree.size();
    }

    @Override
    public List<Book> findInTitleRange(String from, String to) {
        String low = normalize(from);
        String high = normalize(to);
        if (low.compareTo(high) > 0) {
            throw new IllegalArgumentException("Начало диапазона больше конца");
        }
        return tree.findInKeyRange(low, high);
    }

    @Override
    public List<Book> lastAlphabetically(int count) {
        return tree.lastByKey(count);
    }

    @Override
    public List<NodeSnapshot> snapshot() {
        return tree.snapshot(Book::getTitle);
    }

    /** Удаляет все узлы дерева, сохраняя сам объект хранилища. */
    public void clear() {
        tree.clear();
    }

    /** Возвращает книги в порядке возрастания ключа (inorder-обход). */
    public List<Book> inOrderBooks() {
        return tree.inOrderValues();
    }

    /** Ключи узлов, посещённых при последней операции вставки/поиска/удаления. */
    public List<String> getLastVisitedTitles() {
        return tree.getLastVisitedKeys();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
