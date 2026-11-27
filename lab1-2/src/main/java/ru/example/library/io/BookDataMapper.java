package ru.example.library.io;

import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Converts between domain books and the format-neutral serialization DTOs. */
public final class BookDataMapper {
    private BookDataMapper() {
    }

    public static BookCatalogData toData(List<Book> books) {
        List<BookData> result = new ArrayList<>(books.size());
        for (Book book : books) {
            String type = "BOOK";
            String condition = null;
            int course = 0;
            int printRun = 0;
            if (book instanceof AntiqueBook antique) {
                type = "ANTIQUE";
                condition = antique.getCondition();
            } else if (book instanceof Textbook textbook) {
                type = "TEXTBOOK";
                course = textbook.getCourse();
                printRun = textbook.getPrintRun();
            }
            result.add(new BookData(type, book.getIsbn(), book.getTitle(), book.getAuthor(),
                    book.getYear(), book.getGenre(), book.isOnHands(), condition, course, printRun));
        }
        return new BookCatalogData(BookCatalogData.CURRENT_FORMAT_VERSION, result);
    }

    public static List<Book> fromData(BookCatalogData data) throws IOException {
        if (data == null) throw new IOException("Catalog data is missing");
        if (data.formatVersion() != BookCatalogData.CURRENT_FORMAT_VERSION) {
            throw new IOException("Unsupported catalog format version: " + data.formatVersion());
        }
        if (data.books() == null) throw new IOException("Catalog book list is missing");

        List<Book> result = new ArrayList<>(data.books().size());
        for (int i = 0; i < data.books().size(); i++) {
            BookData item = data.books().get(i);
            if (item == null) throw new IOException("Book entry at index " + i + " is null");
            try {
                Book book = switch (item.type() == null ? "" : item.type()) {
                    case "BOOK" -> new Book(item.isbn(), item.title(), item.author(), item.year(),
                            item.genre(), item.onHands());
                    case "ANTIQUE" -> new AntiqueBook(item.isbn(), item.title(), item.author(),
                            item.year(), item.genre(), item.onHands(), item.condition());
                    case "TEXTBOOK" -> new Textbook(item.isbn(), item.title(), item.author(),
                            item.year(), item.genre(), item.onHands(), item.course(), item.printRun());
                    default -> throw new IOException("Unknown book type at index " + i + ": " + item.type());
                };
                List<String> errors = book.validate();
                if (!errors.isEmpty()) {
                    throw new IOException("Invalid book at index " + i + ": " + String.join(", ", errors));
                }
                result.add(book);
            } catch (NullPointerException ex) {
                throw new IOException("Incomplete book data at index " + i, ex);
            }
        }
        return result;
    }
}
