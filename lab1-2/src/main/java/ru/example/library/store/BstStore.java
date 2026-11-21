package ru.example.library.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Универсальное собственное двоичное дерево поиска.
 * Ключ извлекается из элемента, а порядок задаётся Comparable ключа.
 * Дерево не является самобалансирующимся: операции имеют O(h), где h — высота дерева.
 */
public final class BstStore<K extends Comparable<K>, T> {
    private static final class Node<K, T> {
        private K key;
        private T value;
        private Node<K, T> left;
        private Node<K, T> right;

        private Node(K key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    private final Function<? super T, ? extends K> keyExtractor;
    private Node<K, T> root;
    private int size;
    private final List<K> lastVisitedKeys = new ArrayList<>();

    public BstStore(Function<? super T, ? extends K> keyExtractor) {
        this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
    }

    public synchronized void add(T value) {
        Objects.requireNonNull(value, "value");
        K key = extractKey(value);
        lastVisitedKeys.clear();
        if (root == null) {
            root = new Node<>(key, value);
            size = 1;
            lastVisitedKeys.add(key);
            return;
        }

        Node<K, T> current = root;
        while (true) {
            lastVisitedKeys.add(current.key);
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                throw new IllegalArgumentException("Элемент с таким ключом уже существует: " + key);
            }
            if (comparison < 0) {
                if (current.left == null) {
                    current.left = new Node<>(key, value);
                    size++;
                    return;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node<>(key, value);
                    size++;
                    return;
                }
                current = current.right;
            }
        }
    }

    public synchronized T findByKey(K key) {
        Objects.requireNonNull(key, "key");
        lastVisitedKeys.clear();
        Node<K, T> current = root;
        while (current != null) {
            lastVisitedKeys.add(current.key);
            int comparison = key.compareTo(current.key);
            if (comparison == 0) return current.value;
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    public synchronized boolean removeByKey(K key) {
        Objects.requireNonNull(key, "key");
        lastVisitedKeys.clear();
        int previousSize = size;
        root = remove(root, key);
        return size < previousSize;
    }

    private Node<K, T> remove(Node<K, T> node, K key) {
        if (node == null) return null;
        lastVisitedKeys.add(node.key);
        int comparison = key.compareTo(node.key);
        if (comparison < 0) {
            node.left = remove(node.left, key);
        } else if (comparison > 0) {
            node.right = remove(node.right, key);
        } else {
            size--;
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            Node<K, T> successor = minimum(node.right);
            node.key = successor.key;
            node.value = successor.value;
            node.right = removeSuccessor(node.right, successor.key);
        }
        return node;
    }

    private Node<K, T> removeSuccessor(Node<K, T> node, K key) {
        int comparison = key.compareTo(node.key);
        if (comparison < 0) node.left = removeSuccessor(node.left, key);
        else if (comparison > 0) node.right = removeSuccessor(node.right, key);
        else return node.right;
        return node;
    }

    private Node<K, T> minimum(Node<K, T> node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized void clear() {
        root = null;
        size = 0;
        lastVisitedKeys.clear();
    }

    /** Возвращает элементы с ключами в закрытом диапазоне [from, to]. */
    public synchronized List<T> findInKeyRange(K from, K to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException("Начало диапазона больше конца");
        }
        List<T> result = new ArrayList<>();
        collectRange(root, from, to, result);
        return List.copyOf(result);
    }

    private void collectRange(Node<K, T> node, K low, K high, List<T> result) {
        if (node == null) return;
        int lowComparison = node.key.compareTo(low);
        int highComparison = node.key.compareTo(high);
        if (lowComparison > 0) collectRange(node.left, low, high, result);
        if (lowComparison >= 0 && highComparison <= 0) result.add(node.value);
        if (highComparison < 0) collectRange(node.right, low, high, result);
    }

    /** Возвращает не более count элементов в порядке убывания ключа. */
    public synchronized List<T> lastByKey(int count) {
        if (count < 0) throw new IllegalArgumentException("Количество не может быть отрицательным");
        List<T> result = new ArrayList<>(Math.min(count, size));
        collectDescending(root, count, result);
        return List.copyOf(result);
    }

    private void collectDescending(Node<K, T> node, int limit, List<T> result) {
        if (node == null || result.size() >= limit) return;
        collectDescending(node.right, limit, result);
        if (result.size() < limit) result.add(node.value);
        collectDescending(node.left, limit, result);
    }

    /** Снимок узлов в preorder-порядке; ссылки на внутренние узлы не раскрываются. */
    public synchronized List<NodeSnapshot> snapshot() {
        return snapshot(value -> String.valueOf(extractKey(value)));
    }

    /** Снимок с пользовательской подписью узла, например исходным названием сущности. */
    public synchronized List<NodeSnapshot> snapshot(Function<? super T, String> labelExtractor) {
        Objects.requireNonNull(labelExtractor, "labelExtractor");
        List<NodeSnapshot> result = new ArrayList<>(size);
        appendSnapshot(root, -1, false, result, labelExtractor);
        return List.copyOf(result);
    }

    private void appendSnapshot(Node<K, T> node, int parentIndex, boolean left,
                                List<NodeSnapshot> result,
                                Function<? super T, String> labelExtractor) {
        if (node == null) return;
        int index = result.size();
        result.add(new NodeSnapshot(labelExtractor.apply(node.value), parentIndex, left));
        appendSnapshot(node.left, index, true, result, labelExtractor);
        appendSnapshot(node.right, index, false, result, labelExtractor);
    }

    public synchronized List<T> inOrderValues() {
        List<T> result = new ArrayList<>(size);
        collectInOrder(root, result);
        return List.copyOf(result);
    }

    private void collectInOrder(Node<K, T> node, List<T> result) {
        if (node == null) return;
        collectInOrder(node.left, result);
        result.add(node.value);
        collectInOrder(node.right, result);
    }

    public synchronized List<K> getLastVisitedKeys() {
        return List.copyOf(lastVisitedKeys);
    }

    private K extractKey(T value) {
        K key = keyExtractor.apply(value);
        return Objects.requireNonNull(key, "Извлечённый ключ не должен быть null");
    }
}
