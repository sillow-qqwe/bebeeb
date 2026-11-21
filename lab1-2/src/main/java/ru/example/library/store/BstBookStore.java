package ru.example.library.store;

import ru.example.library.model.Book;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Собственное двоичное дерево поиска книг. Дубликаты названий запрещены. */
public final class BstBookStore implements BookStore {
    private static final class Node {
        private Book value;
        private Node left;
        private Node right;
        private Node(Book value) { this.value = value; }
    }

    private Node root;
    private int size;
    private final List<String> lastVisited = new ArrayList<>();

    @Override public synchronized void add(Book book) {
        Objects.requireNonNull(book, "book");
        String key = normalize(book.getTitle());
        if (key.isEmpty()) throw new IllegalArgumentException("Название книги не может быть пустым");
        lastVisited.clear();
        if (root == null) { root = new Node(book); size = 1; lastVisited.add(book.getTitle()); return; }
        Node current = root;
        while (true) {
            lastVisited.add(current.value.getTitle());
            int cmp = compare(key, normalize(current.value.getTitle()));
            if (cmp == 0) throw new IllegalArgumentException("Книга с таким названием уже есть: " + book.getTitle());
            if (cmp < 0) {
                if (current.left == null) { current.left = new Node(book); size++; return; }
                current = current.left;
            } else {
                if (current.right == null) { current.right = new Node(book); size++; return; }
                current = current.right;
            }
        }
    }

    @Override public synchronized Book findByTitle(String title) {
        String key = normalize(title);
        lastVisited.clear();
        Node current = root;
        while (current != null) {
            lastVisited.add(current.value.getTitle());
            int cmp = compare(key, normalize(current.value.getTitle()));
            if (cmp == 0) return current.value;
            current = cmp < 0 ? current.left : current.right;
        }
        return null;
    }

    @Override public synchronized boolean removeByTitle(String title) {
        lastVisited.clear();
        int before = size;
        root = remove(root, normalize(title));
        return size < before;
    }

    private Node remove(Node node, String key) {
        if (node == null) return null;
        lastVisited.add(node.value.getTitle());
        int cmp = compare(key, normalize(node.value.getTitle()));
        if (cmp < 0) node.left = remove(node.left, key);
        else if (cmp > 0) node.right = remove(node.right, key);
        else {
            size--;
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            Node successor = minimum(node.right);
            node.value = successor.value;
            node.right = removeSuccessor(node.right, normalize(successor.value.getTitle()));
        }
        return node;
    }

    private Node removeSuccessor(Node node, String key) {
        int cmp = compare(key, normalize(node.value.getTitle()));
        if (cmp < 0) node.left = removeSuccessor(node.left, key);
        else if (cmp > 0) node.right = removeSuccessor(node.right, key);
        else return node.right;
        return node;
    }

    private Node minimum(Node node) { while (node.left != null) node = node.left; return node; }
    @Override public synchronized int size() { return size; }

    @Override public synchronized List<Book> findInTitleRange(String from, String to) {
        String low = normalize(from), high = normalize(to);
        if (compare(low, high) > 0) throw new IllegalArgumentException("Начало диапазона больше конца");
        List<Book> result = new ArrayList<>();
        collectRange(root, low, high, result);
        return result;
    }
    private void collectRange(Node n, String low, String high, List<Book> out) {
        if (n == null) return;
        String key = normalize(n.value.getTitle());
        int lowCmp = compare(key, low), highCmp = compare(key, high);
        if (lowCmp > 0) collectRange(n.left, low, high, out);
        if (lowCmp >= 0 && highCmp <= 0) out.add(n.value);
        if (highCmp < 0) collectRange(n.right, low, high, out);
    }

    @Override public synchronized List<Book> lastAlphabetically(int count) {
        if (count < 0) throw new IllegalArgumentException("Количество не может быть отрицательным");
        List<Book> result = new ArrayList<>();
        collectDescending(root, count, result);
        return result;
    }
    private void collectDescending(Node n, int limit, List<Book> out) {
        if (n == null || out.size() >= limit) return;
        collectDescending(n.right, limit, out);
        if (out.size() < limit) out.add(n.value);
        collectDescending(n.left, limit, out);
    }

    @Override public synchronized List<NodeSnapshot> snapshot() {
        List<NodeSnapshot> result = new ArrayList<>();
        snapshot(root, -1, false, result);
        return result;
    }
    private void snapshot(Node n, int parent, boolean left, List<NodeSnapshot> out) {
        if (n == null) return;
        int index = out.size();
        out.add(new NodeSnapshot(n.value.getTitle(), parent, left));
        snapshot(n.left, index, true, out);
        snapshot(n.right, index, false, out);
    }


    /** Удаляет все узлы дерева, сохраняя сам объект хранилища. */
    public synchronized void clear() {
        root = null;
        size = 0;
        lastVisited.clear();
    }

    /** Возвращает книги в порядке возрастания ключа (inorder-обход). */
    public synchronized List<Book> inOrderBooks() {
        List<Book> result = new ArrayList<>();
        collectInOrder(root, result);
        return result;
    }

    private void collectInOrder(Node node, List<Book> result) {
        if (node == null) return;
        collectInOrder(node.left, result);
        result.add(node.value);
        collectInOrder(node.right, result);
    }

    public synchronized List<String> getLastVisitedTitles() { return List.copyOf(lastVisited); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
    private static int compare(String a, String b) { return a.compareToIgnoreCase(b); }
}
