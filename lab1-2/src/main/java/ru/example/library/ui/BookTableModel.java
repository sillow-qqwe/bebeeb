package ru.example.library.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

/** Фабрика колонок JavaFX-таблицы каталога. */
public final class BookTableModel {
    private BookTableModel() { }

    public static void configure(TableView<Book> table) {
        table.getColumns().clear();
        table.getColumns().add(column("Тип", book -> {
            if (book instanceof AntiqueBook) return "Антикварная (read-only)";
            if (book instanceof Textbook) return "Учебник (editable)";
            return "Книга";
        }, 175));
        table.getColumns().add(column("ISBN", Book::getIsbn, 125));
        table.getColumns().add(column("Название", Book::getTitle, 190));
        table.getColumns().add(column("Автор", Book::getAuthor, 150));
        table.getColumns().add(column("Год", book -> Integer.toString(book.getYear()), 70));
        table.getColumns().add(column("Жанр", Book::getGenre, 110));
        table.getColumns().add(column("На руках", book -> book.isOnHands() ? "Да" : "Нет", 85));
        table.getColumns().add(column("Дополнительно", book -> {
            if (book instanceof AntiqueBook antique) return antique.getCondition();
            if (book instanceof Textbook textbook)
                return "курс " + textbook.getCourse() + ", тираж " + textbook.getPrintRun();
            return "";
        }, 160));
    }

    private static TableColumn<Book, String> column(
            String title, java.util.function.Function<Book, String> value, double width) {
        TableColumn<Book, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}
