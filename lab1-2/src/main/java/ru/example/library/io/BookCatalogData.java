package ru.example.library.io;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Versioned catalog document shared by JSON and binary serializers. */
public record BookCatalogData(int formatVersion, List<BookData> books) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int CURRENT_FORMAT_VERSION = 1;

    public BookCatalogData {
        books = books == null ? null : new ArrayList<>(books);
    }
}
