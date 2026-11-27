package ru.example.library.io;

import java.io.Serializable;

/** Format-neutral representation of one book, including subtype-specific fields. */
public record BookData(
        String type,
        String isbn,
        String title,
        String author,
        int year,
        String genre,
        boolean onHands,
        String condition,
        int course,
        int printRun
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
