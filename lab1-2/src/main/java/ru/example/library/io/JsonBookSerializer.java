package ru.example.library.io;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Jackson-based JSON serializer for versioned book catalog data. */
public final class JsonBookSerializer implements Serializer<BookCatalogData> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String format() {
        return "json";
    }

    @Override
    public void save(BookCatalogData data, Path file) throws IOException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(file, "file");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
    }

    @Override
    public BookCatalogData load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) throw new IOException("Not a regular file: " + file);
        BookCatalogData data = mapper.readValue(file.toFile(), BookCatalogData.class);
        if (data == null) throw new IOException("JSON catalog is empty");
        if (data.formatVersion() != BookCatalogData.CURRENT_FORMAT_VERSION) {
            throw new IOException("Unsupported catalog format version: " + data.formatVersion());
        }
        if (data.books() == null) throw new IOException("JSON catalog does not contain a books array");
        return data;
    }
}
