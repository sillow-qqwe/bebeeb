package ru.example.library.io;

import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.BookType;
import ru.example.library.model.Textbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/**
 * CSV-сериализатор электронной библиотеки.
 * Формат: разделитель ';', кавычка '"', удвоение кавычек внутри
 * заключённого в кавычки поля. Поля могут содержать разделители и кавычки.
 */
public final class DefaultCsvSerializer implements CsvSerializer {

    private static final char SEP = ';';
    private static final char QUOTE = '"';
    private static final String HEADER =
            "Тип;ISBN;Название;Автор;Год;Жанр;НаРуках;Доп";

    @Override
    public List<Book> load(Path path, List<String> errorLog) throws IOException {
        return load(path, errorLog, ignored -> { });
    }

    @Override
    public List<Book> load(Path path, List<String> errorLog, DoubleConsumer progress) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(errorLog, "errorLog");
        Objects.requireNonNull(progress, "progress");

        List<Book> result = new ArrayList<>();
        long totalBytes = Math.max(1L, Files.size(path));
        long consumedBytes = 0L;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                consumedBytes += line.getBytes(StandardCharsets.UTF_8).length + 1L;
                progress.accept(Math.min(0.99, (double) consumedBytes / totalBytes));
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // UTF-8 BOM
                }
                if (line.isBlank()) {
                    continue;
                }
                if (lineNumber == 1 && line.trim().equalsIgnoreCase(HEADER)) {
                    continue;
                }

                try {
                    List<String> fields = parseCsvLine(line, lineNumber);
                    result.add(parseBook(fields, lineNumber));
                } catch (CsvParseException ex) {
                    errorLog.add(ex.getMessage());
                }
            }
        }
        progress.accept(1.0);
        return result;
    }

    private Book parseBook(List<String> fields, int lineNumber)
            throws CsvParseException {
        if (fields.size() != 8) {
            throw new CsvParseException(lineNumber, CsvErrorCode.WRONG_FIELD_COUNT,
                    "найдено колонок: " + fields.size() + ", ожидалось: 8");
        }

        BookType type = BookType.fromString(fields.get(0).trim());
        if (type == null) {
            throw new CsvParseException(lineNumber, CsvErrorCode.UNKNOWN_TYPE,
                    "неизвестный тип: '" + fields.get(0) + "'");
        }

        String isbn = fields.get(1).trim();
        String title = fields.get(2).trim();
        String author = fields.get(3).trim();
        int year = parseInt(fields.get(4), lineNumber, "год");
        String genre = fields.get(5).trim();
        boolean onHands = parseBoolean(fields.get(6), lineNumber);
        String extra = fields.get(7).trim();

        Book book;
        switch (type) {
            case BOOK -> book = new Book(isbn, title, author, year, genre, onHands);
            case ANTIQUE -> book = new AntiqueBook(
                    isbn, title, author, year, genre, onHands, extra);
            case TEXTBOOK -> {
                String[] parts = extra.split(":", -1);
                if (parts.length != 2) {
                    throw new CsvParseException(lineNumber, CsvErrorCode.BAD_NUMBER,
                            "ожидалось дополнительное поле 'курс:тираж', получено: '" + extra + "'");
                }
                int course = parseInt(parts[0], lineNumber, "курс");
                int printRun = parseInt(parts[1], lineNumber, "тираж");
                book = new Textbook(isbn, title, author, year, genre,
                        onHands, course, printRun);
            }
            default -> throw new IllegalStateException("Необработанный тип книги: " + type);
        }

        List<String> validationErrors = book.validate();
        if (!validationErrors.isEmpty()) {
            throw new CsvParseException(lineNumber, CsvErrorCode.INVALID_DATA,
                    String.join(", ", validationErrors));
        }
        return book;
    }

    /**
     * Разбирает одну физическую строку CSV.
     * Кавычки допустимы в начале поля; внутри quoted-поля "" означает ".
     */
    private List<String> parseCsvLine(String line, int lineNumber)
            throws CsvParseException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteClosed = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == QUOTE) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                        field.append(QUOTE);
                        i++;
                    } else {
                        inQuotes = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }

            if (quoteClosed) {
                if (ch == SEP) {
                    fields.add(field.toString());
                    field.setLength(0);
                    quoteClosed = false;
                } else {
                    throw malformed(lineNumber,
                            "после закрывающей кавычки ожидался разделитель");
                }
                continue;
            }

            if (ch == SEP) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (ch == QUOTE) {
                if (field.length() != 0) {
                    throw malformed(lineNumber,
                            "кавычка внутри незакавыченного поля");
                }
                inQuotes = true;
            } else {
                field.append(ch);
            }
        }

        if (inQuotes) {
            throw malformed(lineNumber, "не закрыта кавычка");
        }

        fields.add(field.toString());
        return fields;
    }

    private CsvParseException malformed(int lineNumber, String details) {
        return new CsvParseException(lineNumber, CsvErrorCode.MALFORMED_CSV, details);
    }

    private int parseInt(String raw, int lineNumber, String field)
            throws CsvParseException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new CsvParseException(lineNumber, CsvErrorCode.BAD_NUMBER,
                    "некорректный " + field + ": '" + raw + "'");
        }
    }

    private boolean parseBoolean(String raw, int lineNumber)
            throws CsvParseException {
        String value = raw.trim();
        if (value.equals("1") || value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equals("0") || value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new CsvParseException(lineNumber, CsvErrorCode.BAD_BOOLEAN,
                "получено: '" + raw + "'");
    }

    @Override
    public void save(Path path, List<Book> books) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(books, "books");

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Book book : books) {
                Objects.requireNonNull(book, "Список книг не должен содержать null");
                List<String> fields = new ArrayList<>(8);
                fields.add(typeOf(book).name());
                fields.add(nullToEmpty(book.getIsbn()));
                fields.add(nullToEmpty(book.getTitle()));
                fields.add(nullToEmpty(book.getAuthor()));
                fields.add(Integer.toString(book.getYear()));
                fields.add(nullToEmpty(book.getGenre()));
                fields.add(book.isOnHands() ? "1" : "0");

                if (book instanceof AntiqueBook antique) {
                    fields.add(nullToEmpty(antique.getCondition()));
                } else if (book instanceof Textbook textbook) {
                    fields.add(textbook.getCourse() + ":" + textbook.getPrintRun());
                } else {
                    fields.add("");
                }

                for (int i = 0; i < fields.size(); i++) {
                    if (i > 0) {
                        writer.write(SEP);
                    }
                    writer.write(escape(fields.get(i)));
                }
                writer.newLine();
            }
        }
    }


    private String escape(String value) throws IOException {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IOException(
                    "Переводы строк внутри полей CSV не поддерживаются"
            );
        }

        boolean needsQuotes = value.indexOf(SEP) >= 0
                || value.indexOf(QUOTE) >= 0
                || value.startsWith(" ")
                || value.endsWith(" ");

        if (!needsQuotes) {
            return value;
        }

        return QUOTE + value.replace("\"", "\"\"") + QUOTE;
    }


    private static BookType typeOf(Book book) {
        if (book instanceof AntiqueBook) return BookType.ANTIQUE;
        if (book instanceof Textbook) return BookType.TEXTBOOK;
        return BookType.BOOK;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
