
package ru.example.library.model;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    void allBookFieldsCanBeChanged() {
        Book book = new Book(
                "1234567890",
                "Старое название",
                "Старый автор",
                2000,
                "Роман",
                false
        );

        book.setIsbn("9876543210");
        book.setTitle("Новое название");
        book.setAuthor("Новый автор");
        book.setYear(2020);
        book.setGenre("Фантастика");
        book.setOnHands(true);

        assertAll(
                () -> assertEquals("9876543210", book.getIsbn()),
                () -> assertEquals("Новое название", book.getTitle()),
                () -> assertEquals("Новый автор", book.getAuthor()),
                () -> assertEquals(2020, book.getYear()),
                () -> assertEquals("Фантастика", book.getGenre()),
                () -> assertTrue(book.isOnHands())
        );
    }

    @Test
    void validBookHasNoValidationErrors() {
        Book book = new Book(
                "1234567890",
                "Война и мир",
                "Лев Толстой",
                1869,
                "Роман",
                false
        );

        assertTrue(book.validate().isEmpty());
    }

    @Test
    void emptyFieldsProduceValidationErrors() {
        Book book = new Book(
                "",
                " ",
                null,
                999,
                "",
                false
        );

        assertEquals(5, book.validate().size());
    }

    @Test
    void futureYearIsRejected() {
        int futureYear = Year.now().getValue() + 1;

        Book book = new Book(
                "1234567890",
                "Название",
                "Автор",
                futureYear,
                "Роман",
                false
        );

        assertFalse(book.validate().isEmpty());
    }

    @Test
    void yearAtCurrentLimitIsAccepted() {
        int currentYear = Year.now().getValue();

        Book book = new Book(
                "1234567890",
                "Название",
                "Автор",
                currentYear,
                "Роман",
                false
        );

        assertTrue(book.validate().isEmpty());
    }

    @Test
    void yearBelowMinimumIsRejected() {
        Book book = new Book(
                "1234567890",
                "Название",
                "Автор",
                999,
                "Роман",
                false
        );

        assertFalse(book.validate().isEmpty());
    }
}
