package ru.example.library.store;

import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.IntConsumer;

/** Генератор тестового каталога с уникальными названиями и ISBN. */
public final class BookGenerator {
    private static final String[] AUTHORS = {"А. Иванов", "М. Петрова", "С. Сидоров", "Е. Смирнова", "Д. Волков"};
    private static final String[] GENRES = {"Фантастика", "История", "Программирование", "Роман", "Наука"};
    private static final String[] WORDS = {"Алгоритм", "Галактика", "Память", "Горизонт", "Механизм", "Архив", "Система", "Путешествие"};

    private BookGenerator() { }

    public static List<Book> generate(int count) {
        return generate(count, ignored -> { });
    }

    public static List<Book> generate(int count, IntConsumer progress) {
        if (count < 1 || count > 100_000) {
            throw new IllegalArgumentException("Количество книг должно быть от 1 до 100000.");
        }
        if (progress == null) throw new NullPointerException("progress");
        Random random = new Random();
        long batchId = Math.abs(random.nextLong() % 1_000_000_000L);
        List<Book> result = new ArrayList<>(count);
        int currentYear = Year.now().getValue();
        for (int i = 1; i <= count; i++) {
            String title = String.format(Locale.ROOT, "%s %09d-%05d", WORDS[random.nextInt(WORDS.length)], batchId, i);
            String isbn = String.format(Locale.ROOT, "978%010d", (batchId + i) % 10_000_000_000L);
            int year = 1950 + random.nextInt(Math.max(1, currentYear - 1949));
            boolean textbook = i % 5 == 0;
            Book book = textbook
                    ? new Textbook(isbn, title, AUTHORS[random.nextInt(AUTHORS.length)], year,
                        GENRES[random.nextInt(GENRES.length)], random.nextBoolean(), 1 + random.nextInt(5), 500 + random.nextInt(9500))
                    : new Book(isbn, title, AUTHORS[random.nextInt(AUTHORS.length)], year,
                        GENRES[random.nextInt(GENRES.length)], random.nextBoolean());
            result.add(book);
            if (i % 100 == 0 || i == count) progress.accept(i);
        }
        return result;
    }
}
