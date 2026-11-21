
package ru.example.library.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextbookTest {

    @Test
    void textbookCanBeCreated() {
        Textbook textbook = new Textbook(
                "1234567890",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                2,
                1000
        );

        assertEquals("Основы Java", textbook.getTitle());
        assertEquals(2, textbook.getCourse());
        assertEquals(1000, textbook.getPrintRun());
    }

    @Test
    void textbookInheritsBookEditing() {
        Textbook textbook = new Textbook(
                "1234567890",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                2,
                1000
        );

        textbook.setTitle("Продвинутый Java");
        textbook.setAuthor("Пётр Петров");
        textbook.setYear(2025);
        textbook.setCourse(3);
        textbook.setPrintRun(2000);

        assertEquals("Продвинутый Java", textbook.getTitle());
        assertEquals("Пётр Петров", textbook.getAuthor());
        assertEquals(2025, textbook.getYear());
        assertEquals(3, textbook.getCourse());
        assertEquals(2000, textbook.getPrintRun());
    }

    @Test
    void validTextbookHasNoErrors() {
        Textbook textbook = new Textbook(
                "1234567890",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                2,
                1000
        );

        assertTrue(textbook.validate().isEmpty());
    }

    @Test
    void invalidCourseIsRejected() {
        Textbook textbook = new Textbook(
                "1234567890",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                7,
                1000
        );

        assertTrue(textbook.validate().contains(
                "Курс должен быть от 1 до 5"
        ));
    }

    @Test
    void invalidPrintRunIsRejected() {
        Textbook textbook = new Textbook(
                "1234567890",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                2,
                0
        );

        assertTrue(textbook.validate().contains(
                "Тираж должен быть положительным числом"
        ));
    }

    @Test
    void multipleErrorsAreDetected() {
        Textbook textbook = new Textbook(
                "",
                "Основы Java",
                "Иван Иванов",
                2024,
                "Программирование",
                false,
                0,
                -10
        );

        assertEquals(3, textbook.validate().size());
    }
}
