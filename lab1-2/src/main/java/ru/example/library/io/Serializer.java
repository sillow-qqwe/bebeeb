package ru.example.library.io;

import java.io.IOException;
import java.nio.file.Path;

/** Common contract for reading and writing a data object in a particular format. */
public interface Serializer<T> {
    String format();

    void save(T data, Path file) throws IOException;

    T load(Path file) throws IOException;
}
