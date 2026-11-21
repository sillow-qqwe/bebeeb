package ru.example.library.store;

import org.junit.jupiter.api.Test;
import ru.example.library.model.Book;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BstBookStoreTest {
    private Book book(String title) { return new Book("ISBN-" + title, title, "Автор", 2000, "Жанр", false); }
    @Test void insertsFindsAndCountsBooks() {
        BstBookStore s = new BstBookStore(); s.add(book("Мартин Иден")); s.add(book("Война и мир")); s.add(book("Эффективная Java"));
        assertEquals(3, s.size()); assertEquals("Война и мир", s.findByTitle("война и мир").getTitle()); assertNull(s.findByTitle("Нет"));
    }
    @Test void rejectsDuplicateTitle() { BstBookStore s = new BstBookStore(); s.add(book("Дюна")); assertThrows(IllegalArgumentException.class, () -> s.add(book("дюна"))); }
    @Test void removesNodesAndMaintainsSize() {
        BstBookStore s = new BstBookStore(); for (String t : List.of("М", "В", "Т", "А", "Г", "П", "Я")) s.add(book(t));
        assertTrue(s.removeByTitle("А")); assertTrue(s.removeByTitle("В")); assertTrue(s.removeByTitle("М")); assertFalse(s.removeByTitle("Нет"));
        assertEquals(4, s.size()); assertNull(s.findByTitle("М"));
    }
    @Test void exposesInOrderProjectionAndCanBeCleared() {
        BstBookStore s = new BstBookStore();
        s.add(book("М")); s.add(book("А")); s.add(book("Я"));
        assertEquals(List.of("А", "М", "Я"), s.inOrderBooks().stream().map(Book::getTitle).toList());
        s.clear();
        assertEquals(0, s.size());
        assertTrue(s.snapshot().isEmpty());
        assertTrue(s.inOrderBooks().isEmpty());
    }

    @Test void rangeAndKLastAreAlphabetical() {
        BstBookStore s = new BstBookStore(); for (String t : List.of("Г", "А", "Е", "Б", "Д", "В")) s.add(book(t));
        assertEquals(List.of("Б", "В", "Г", "Д"), s.findInTitleRange("Б", "Д").stream().map(Book::getTitle).toList());
        assertEquals(List.of("Е", "Д"), s.lastAlphabetically(2).stream().map(Book::getTitle).toList());
    }
}
