package ru.example.library.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;

/** Common contract for reading and writing a data object in a particular format. */
public interface Serializer<T> {
    String format();

    void save(T data, Path file) throws IOException;

    T load(Path file) throws IOException;

    default T load(Path file, DoubleConsumer progress) throws IOException {
        T result = load(file);
        progress.accept(1.0);
        return result;
    }
}
