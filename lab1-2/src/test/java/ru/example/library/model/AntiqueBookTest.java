
package ru.example.library.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiqueBookTest {

    private AntiqueBook createBook() {
        return new AntiqueBook(
                "1234567890",
                "Старинная книга",
                "Неизвестный автор",
                1850,
                "История",
                false,
                "Хорошее"
        );
    }

    @Test
    void antiqueBookCanBeCreated() {
        AntiqueBook book = createBook();

        assertEquals("Старинная книга", book.getTitle());
        assertEquals("Неизвестный автор", book.getAuthor());
        assertEquals(1850, book.getYear());
        assertEquals("Хорошее", book.getCondition());
    }

    @Test
    void antiqueBookCannotChangeIsbn() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setIsbn("9999999999")
        );
    }

    @Test
    void antiqueBookCannotChangeTitle() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setTitle("Новое название")
        );
    }

    @Test
    void antiqueBookCannotChangeAuthor() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setAuthor("Новый автор")
        );
    }

    @Test
    void antiqueBookCannotChangeYear() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setYear(1900)
        );
    }

    @Test
    void antiqueBookCannotChangeGenre() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setGenre("Другой жанр")
        );
    }

    @Test
    void antiqueBookCannotChangeOnHands() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setOnHands(true)
        );
    }

    @Test
    void antiqueBookFieldsRemainUnchangedAfterFailedEditing() {
        AntiqueBook book = createBook();

        assertThrows(
                UnsupportedOperationException.class,
                () -> book.setTitle("Изменённое название")
        );

        assertEquals("Старинная книга", book.getTitle());
    }

    @Test
    void emptyConditionIsRejected() {
        AntiqueBook book = new AntiqueBook(
                "1234567890",
                "Старинная книга",
                "Автор",
                1850,
                "История",
                false,
                ""
        );

        assertTrue(book.validate().contains(
                "Необходимо указать состояние сохранности антикварной книги"
        ));
    }

    @Test
    void validAntiqueBookHasNoErrors() {
        AntiqueBook book = createBook();

        assertTrue(book.validate().isEmpty());
    }
}
