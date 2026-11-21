package ru.example.library.model;

public enum BookType {
    BOOK,
    ANTIQUE,
    TEXTBOOK;

    public static BookType fromString(String s) {
        return switch (s.trim().toUpperCase()) {
            case "BOOK" -> BOOK;
            case "ANTIQUE" -> ANTIQUE;
            case "TEXTBOOK" -> TEXTBOOK;
            default -> null;
        };
    }
}
