package ru.example.library.io;

import ru.example.library.model.Book;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.DoubleConsumer;

public interface CsvSerializer {
    List<Book> load(Path path, List<String> errorLog) throws IOException;

    default List<Book> load(Path path, List<String> errorLog, DoubleConsumer progress) throws IOException {
        List<Book> result = load(path, errorLog);
        progress.accept(1.0);
        return result;
    }
    void save(Path path, List<Book> books) throws IOException;
}