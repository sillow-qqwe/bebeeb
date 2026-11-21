package ru.example.library.store;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BstStoreTest {
    @Test
    void storesValuesByGenericComparableKey() {
        BstStore<String, String> tree = new BstStore<>(String::toLowerCase);
        tree.add("Mango");
        tree.add("Apple");
        tree.add("Pear");

        assertEquals(3, tree.size());
        assertEquals("Apple", tree.findByKey("APPLE"));
        assertNull(tree.findByKey("missing"));
        assertThrows(IllegalArgumentException.class, () -> tree.add("APPLE"));
    }

    @Test
    void supportsRangeDescendingTraversalAndRemoval() {
        BstStore<Integer, String> tree = new BstStore<>(String::length);
        tree.add("a");
        tree.add("bbbb");
        tree.add("cc");
        tree.add("ddd");

        assertEquals(List.of("cc", "ddd"), tree.findInKeyRange(2, 3));
        assertEquals(List.of("bbbb", "ddd"), tree.lastByKey(2));
        assertTrue(tree.removeByKey(2));
        assertFalse(tree.removeByKey(2));
        assertEquals(3, tree.size());
    }

    @Test
    void snapshotsAreImmutableAndUseProvidedLabels() {
        BstStore<String, String> tree = new BstStore<>(String::toLowerCase);
        tree.add("Zulu");
        tree.add("Alpha");

        List<NodeSnapshot> snapshot = tree.snapshot(String::toUpperCase);
        assertEquals("ZULU", snapshot.get(0).title());
        assertEquals("ALPHA", snapshot.get(1).title());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new NodeSnapshot("X", -1, false)));
    }

    @Test
    void clearResetsTreeAndVisitedKeys() {
        BstStore<String, String> tree = new BstStore<>(String::toLowerCase);
        tree.add("one");
        tree.findByKey("one");
        assertFalse(tree.getLastVisitedKeys().isEmpty());

        tree.clear();

        assertEquals(0, tree.size());
        assertTrue(tree.snapshot().isEmpty());
        assertTrue(tree.getLastVisitedKeys().isEmpty());
    }
}
