package ru.example.library.io;

import ru.example.library.model.Book;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/** Adapter that exposes the existing CSV implementation through the common Serializer contract. */
public final class CsvBookSerializer implements Serializer<BookCatalogData> {
    private final CsvSerializer delegate = new DefaultCsvSerializer();

    @Override
    public String format() {
        return "csv";
    }

    @Override
    public void save(BookCatalogData data, Path file) throws IOException {
        delegate.save(file, BookDataMapper.fromData(data));
    }

    @Override
    public BookCatalogData load(Path file) throws IOException {
        return load(file, ignored -> { });
    }

    @Override
    public BookCatalogData load(Path file, DoubleConsumer progress) throws IOException {
        List<String> errors = new ArrayList<>();
        List<Book> books = delegate.load(file, errors, progress);
        if (!errors.isEmpty()) {
            throw new IOException("CSV contains invalid rows: " + String.join("; ", errors));
        }
        return BookDataMapper.toData(books);
    }
}
