package ru.example.library.store;

import org.junit.jupiter.api.Test;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class BookGeneratorTest {
    @Test
    void generatesRequestedNumberOfBooksWithUniqueKeysAndIsbns() {
        var books = BookGenerator.generate(125);
        assertEquals(125, books.size());
        assertEquals(125, new HashSet<>(books.stream().map(Book::getTitle).toList()).size());
        assertEquals(125, new HashSet<>(books.stream().map(Book::getIsbn).toList()).size());
        assertTrue(books.stream().allMatch(book -> book.validate().isEmpty()));
        assertTrue(books.stream().anyMatch(Textbook.class::isInstance));
    }

    @Test
    void rejectsInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> BookGenerator.generate(0));
        assertThrows(IllegalArgumentException.class, () -> BookGenerator.generate(100_001));
    }
}
