package ru.example.library.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** Базовая полностью редактируемая сущность */
public class Book implements Editable {
    private String isbn;
    private String title;
    private String author;
    private int year;
    private String genre;
    private boolean onHands;

    public Book(String isbn, String title, String author, int year,
                String genre, boolean onHands) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.year = year;
        this.genre = genre;
        this.onHands = onHands;
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public boolean isOnHands() { return onHands; }
    public void setOnHands(boolean onHands) { this.onHands = onHands; }

    /** Общая проверка полей, применимая ко всем разновидностям книг. */
    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (isBlank(isbn)) errors.add("ISBN не может быть пустым");
        if (isBlank(title)) errors.add("Название не может быть пустым");
        if (isBlank(author)) errors.add("Автор не может быть пустым");
        if (isBlank(genre)) errors.add("Жанр не может быть пустым");
        int currentYear = Year.now().getValue();
        if (year < 1000 || year > currentYear) {
            errors.add("Год издания должен быть от 1000 до " + currentYear);
        }
        return errors;
    }

    protected static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public String toString() {
        return "Book{isbn='%s', title='%s', author='%s', year=%d, genre='%s', onHands=%s}"
                .formatted(isbn, title, author, year, genre, onHands);
    }
}