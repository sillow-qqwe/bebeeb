package ru.example.library.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCsvSerializerTest {

    @TempDir
    Path tempDir;

    private final DefaultCsvSerializer serializer = new DefaultCsvSerializer();

    @Test
    void loadsAllThreeBookTypes() throws IOException {
        Path file = tempDir.resolve("books.csv");
        Files.writeString(file, """
                Тип;ISBN;Название;Автор;Год;Жанр;НаРуках;Доп
                BOOK;1234567890;Обычная;Автор;2000;Роман;0;
                ANTIQUE;1234567891;Старинная;Автор;1850;История;1;Хорошее
                TEXTBOOK;1234567892;Java;Автор;2020;Обучение;0;2:500
                """);

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertEquals(3, books.size());
        assertTrue(errors.isEmpty());
        assertEquals(Book.class, books.get(0).getClass());
        assertInstanceOf(AntiqueBook.class, books.get(1));
        assertInstanceOf(Textbook.class, books.get(2));
        assertEquals("Хорошее", ((AntiqueBook) books.get(1)).getCondition());
        assertEquals(2, ((Textbook) books.get(2)).getCourse());
        assertEquals(500, ((Textbook) books.get(2)).getPrintRun());
    }

    @Test
    void parsesDelimiterInsideQuotedField() throws IOException {
        Path file = tempDir.resolve("quoted-delimiter.csv");
        Files.writeString(file, """
                Тип;ISBN;Название;Автор;Год;Жанр;НаРуках;Доп
                BOOK;1234567890;"Java; основы программирования";Автор;2024;Обучение;0;
                """);

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(errors.isEmpty());
        assertEquals(1, books.size());
        assertEquals("Java; основы программирования", books.get(0).getTitle());
    }

    @Test
    void parsesEscapedQuotesInsideField() throws IOException {
        Path file = tempDir.resolve("quotes.csv");
        Files.writeString(file,
                "BOOK;1234567890;\"Книга \"\"Java\"\"\";Автор;2024;Обучение;0;");


        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(errors.isEmpty());
        assertEquals("Книга \"Java\"", books.get(0).getTitle());
    }

    @Test
    void saveAndLoadPreserveAllBookTypesAndSpecialCharacters() throws IOException {
        Path file = tempDir.resolve("round-trip.csv");
        List<Book> source = List.of(
                new Book("1234567890", "Java; основы", "Автор \"А\"", 2024,
                        "Программирование", false),
                new AntiqueBook("1234567891", "Старая книга", "Автор", 1850,
                        "История", true, "Хорошее; без повреждений"),
                new Textbook("1234567892", "Алгоритмы", "Кормен", 2020,
                        "Информатика", false, 2, 1500)
        );

        serializer.save(file, source);

        List<String> errors = new ArrayList<>();
        List<Book> loaded = serializer.load(file, errors);

        assertTrue(errors.isEmpty());
        assertEquals(3, loaded.size());
        assertEquals(source.get(0).getTitle(), loaded.get(0).getTitle());
        assertEquals(source.get(0).getAuthor(), loaded.get(0).getAuthor());
        assertEquals("Хорошее; без повреждений",
                ((AntiqueBook) loaded.get(1)).getCondition());
        assertInstanceOf(Textbook.class, loaded.get(2));
        assertEquals(1500, ((Textbook) loaded.get(2)).getPrintRun());
    }

    @Test
    void malformedRowIsSkippedAndErrorIsLogged() throws IOException {
        Path file = tempDir.resolve("malformed.csv");
        Files.writeString(file, """
                BOOK;1234567890;Корректная;Автор;2020;Роман;0;
                BOOK;1234567891;"Незакрытая кавычка;Автор;2020;Роман;0;
                BOOK;1234567892;Ещё одна;Автор;2021;Роман;0;
                """);

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertEquals(2, books.size());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Строка 2"));
        assertTrue(errors.get(0).contains("Нарушен синтаксис CSV"));
    }

    @Test
    void unknownTypeIsSkippedWithDiagnostic() throws IOException {
        Path file = tempDir.resolve("unknown-type.csv");
        Files.writeString(file, "ALIEN;1234567890;Название;Автор;2020;Роман;0;\n");

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(books.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("неизвестный тип"));
    }

    @Test
    void invalidNumberIsSkippedWithDiagnostic() throws IOException {
        Path file = tempDir.resolve("bad-number.csv");
        Files.writeString(file, "BOOK;1234567890;Название;Автор;год;Роман;0;\n");

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(books.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("некорректный год"));
    }

    @Test
    void invalidBooleanIsSkippedWithDiagnostic() throws IOException {
        Path file = tempDir.resolve("bad-boolean.csv");
        Files.writeString(file, "BOOK;1234567890;Название;Автор;2020;Роман;yes;\n");

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(books.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Ошибка преобразования boolean"));
    }

    @Test
    void wrongColumnCountIsSkipped() throws IOException {
        Path file = tempDir.resolve("wrong-count.csv");
        Files.writeString(file, "BOOK;1234567890;Название;Автор;2020;Роман;0\n");

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(books.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Неверное количество колонок"));
    }

    @Test
    void emptyBooleanIsNotSilentlyAccepted() throws IOException {
        Path file = tempDir.resolve("empty-boolean.csv");
        Files.writeString(file, "BOOK;1234567890;Название;Автор;2020;Роман;;\n");

        List<String> errors = new ArrayList<>();
        List<Book> books = serializer.load(file, errors);

        assertTrue(books.isEmpty());
        assertEquals(1, errors.size());
    }

    @Test
    void savingFieldWithLineBreakFailsClearly() {
        String title = "Название" + '\n' + "с продолжением";

        Book book = new Book(
                "1234567890",
                title,
                "Автор",
                2024,
                "Жанр",
                false
        );

        // Убеждаемся, что в названии действительно есть перенос строки.
        assertTrue(title.contains("\n"));

        assertThrows(
                IOException.class,
                () -> serializer.save(
                        tempDir.resolve("line-break.csv"),
                        List.of(book)
                )
        );
    }

}