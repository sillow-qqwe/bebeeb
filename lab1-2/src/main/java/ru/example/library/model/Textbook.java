package ru.example.library.model;

import java.util.ArrayList;
import java.util.List;

/** Учебник: полностью редактируемая книга с дополнительными характеристиками. */
public class Textbook extends Book {
    private int course;
    private int printRun;

    public Textbook(String isbn, String title, String author, int year,
                    String genre, boolean onHands, int course, int printRun) {
        super(isbn, title, author, year, genre, onHands);
        this.course = course;
        this.printRun = printRun;
    }

    public int getCourse() { return course; }
    public void setCourse(int course) { this.course = course; }

    public int getPrintRun() { return printRun; }
    public void setPrintRun(int printRun) { this.printRun = printRun; }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(super.validate());
        if (course < 1 || course > 5) errors.add("Курс должен быть от 1 до 5");
        if (printRun <= 0) errors.add("Тираж должен быть положительным числом");
        return errors;
    }

    @Override
    public String toString() {
        return "Textbook{" + super.toString() + ", course=" + course + ", printRun=" + printRun + "}";
    }
}
