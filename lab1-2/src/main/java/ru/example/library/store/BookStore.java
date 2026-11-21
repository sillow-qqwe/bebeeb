package ru.example.library.store;

import ru.example.library.model.Book;
import java.util.List;

/** Контракт хранилища книг. Ключом является название книги. */
public interface BookStore {
    void add(Book book);
    Book findByTitle(String title);
    boolean removeByTitle(String title);
    int size();
    List<Book> findInTitleRange(String fromInclusive, String toInclusive);
    List<Book> lastAlphabetically(int count);
    List<NodeSnapshot> snapshot();
}
