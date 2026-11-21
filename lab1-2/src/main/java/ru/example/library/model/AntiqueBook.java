package ru.example.library.model;

import java.util.ArrayList;
import java.util.List;

/** Антикварная книга доступна только для чтения. */
public class AntiqueBook extends Book {
    private final String condition;

    public AntiqueBook(String isbn, String title, String author, int year,
                       String genre, boolean onHands, String condition) {
        super(isbn, title, author, year, genre, onHands);
        this.condition = condition;
    }

    public String getCondition() { return condition; }

    /* Запрещаем изменение унаследованных полей даже при прямом обращении к объекту. */
    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("Антикварная книга доступна только для чтения");
    }

    @Override public void setIsbn(String value) { throw readOnly(); }
    @Override public void setTitle(String value) { throw readOnly(); }
    @Override public void setAuthor(String value) { throw readOnly(); }
    @Override public void setYear(int value) { throw readOnly(); }
    @Override public void setGenre(String value) { throw readOnly(); }
    @Override public void setOnHands(boolean value) { throw readOnly(); }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(super.validate());
        if (isBlank(condition)) errors.add("Необходимо указать состояние сохранности антикварной книги");
        return errors;
    }

    @Override
    public String toString() {
        return "AntiqueBook{" + super.toString() + ", condition='" + condition + "'}";
    }
}
