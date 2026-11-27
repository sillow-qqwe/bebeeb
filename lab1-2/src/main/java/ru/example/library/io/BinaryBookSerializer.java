package ru.example.library.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** JDK object-stream serializer with a magic header and explicit format version. */
public final class BinaryBookSerializer implements Serializer<BookCatalogData> {
    private static final int MAGIC = 0x4C494252; // LIBR
    private static final int FORMAT_VERSION = BookCatalogData.CURRENT_FORMAT_VERSION;

    @Override
    public String format() {
        return "bin";
    }

    @Override
    public void save(BookCatalogData data, Path file) throws IOException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(file, "file");
        if (data.formatVersion() != FORMAT_VERSION) {
            throw new IOException("Unsupported catalog format version: " + data.formatVersion());
        }
        try (var raw = new BufferedOutputStream(Files.newOutputStream(file))) {
            var header = new java.io.DataOutputStream(raw);
            header.writeInt(MAGIC);
            header.writeInt(FORMAT_VERSION);
            header.flush();
            try (ObjectOutputStream objects = new ObjectOutputStream(raw)) {
                objects.writeObject(data);
            }
        }
    }

    @Override
    public BookCatalogData load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try (var raw = new BufferedInputStream(Files.newInputStream(file))) {
            var header = new java.io.DataInputStream(raw);
            int magic = header.readInt();
            if (magic != MAGIC) throw new IOException("Invalid binary catalog signature");
            int version = header.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported binary format version: " + version);
            }
            try (ObjectInputStream objects = new ObjectInputStream(raw)) {
                objects.setObjectInputFilter(info -> {
                    if (info.depth() > 30 || info.references() > 5_000_000
                            || info.arrayLength() > 1_000_000) {
                        return ObjectInputFilter.Status.REJECTED;
                    }
                    Class<?> type = info.serialClass();
                    if (type == null || type.isPrimitive()) return ObjectInputFilter.Status.UNDECIDED;
                    if (type.isArray()) {
                        Class<?> component = type.getComponentType();
                        return component == Object.class || component == String.class
                                ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
                    }
                    String name = type.getName();
                    if (name.startsWith("ru.example.library.io.")
                            || name.startsWith("java.util.")
                            || name.equals("java.lang.Object")
                            || name.equals("java.lang.String")
                            || name.equals("java.lang.Integer")) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }
                    return ObjectInputFilter.Status.REJECTED;
                });
                Object value = objects.readObject();
                if (!(value instanceof BookCatalogData data)) {
                    throw new IOException("Binary file does not contain a book catalog");
                }
                if (data.formatVersion() != version) {
                    throw new IOException("Header and catalog versions do not match");
                }
                if (data.books() == null) throw new IOException("Binary catalog has no book list");
                return data;
            } catch (ClassNotFoundException ex) {
                throw new IOException("Unknown class in binary catalog", ex);
            }
        } catch (java.io.EOFException ex) {
            throw new IOException("Binary catalog is truncated or empty", ex);
        }
    }
}
