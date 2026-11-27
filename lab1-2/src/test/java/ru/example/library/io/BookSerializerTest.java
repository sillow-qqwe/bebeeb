package ru.example.library.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookSerializerTest {
    @TempDir Path tempDir;

    private final JsonBookSerializer json = new JsonBookSerializer();
    private final BinaryBookSerializer binary = new BinaryBookSerializer();

    private List<Book> sampleBooks() {
        return List.of(
                new Book("9780000000001", "Ordinary book", "Author One", 2020, "Fiction", false),
                new AntiqueBook("9780000000002", "Antique book", "Author Two", 1850,
                        "History", true, "Good condition"),
                new Textbook("9780000000003", "Java basics", "Author Three", 2023,
                        "Education", false, 2, 1200)
        );
    }

    private BookCatalogData sampleData() {
        return BookDataMapper.toData(sampleBooks());
    }

    @Test
    void jsonRoundTripPreservesAllBookTypesAndFields() throws IOException {
        Path file = tempDir.resolve("catalog.json");
        json.save(sampleData(), file);
        assertEquals(sampleData(), json.load(file));
        assertTrue(Files.readString(file).contains("\"formatVersion\""));
    }

    @Test
    void binaryRoundTripPreservesAllBookTypesAndFields() throws IOException {
        Path file = tempDir.resolve("catalog.bin");
        binary.save(sampleData(), file);
        assertEquals(sampleData(), binary.load(file));
    }

    @Test
    void mapperRoundTripPreservesSubtypeInformation() throws IOException {
        List<Book> restored = BookDataMapper.fromData(sampleData());
        assertEquals(Book.class, restored.get(0).getClass());
        assertInstanceOf(AntiqueBook.class, restored.get(1));
        assertEquals("Good condition", ((AntiqueBook) restored.get(1)).getCondition());
        assertInstanceOf(Textbook.class, restored.get(2));
        assertEquals(2, ((Textbook) restored.get(2)).getCourse());
        assertEquals(1200, ((Textbook) restored.get(2)).getPrintRun());
    }

    @Test
    void loadsPreparedJsonFixtureFromClasspath() throws IOException {
        try (var input = getClass().getResourceAsStream("/catalog-v1.json")) {
            assertNotNull(input);
            Path file = tempDir.resolve("fixture.json");
            Files.copy(input, file);
            BookCatalogData loaded = json.load(file);
            List<Book> books = BookDataMapper.fromData(loaded);
            assertEquals(1, books.size());
            assertEquals("Fixture book", books.get(0).getTitle());
        }
    }

    @Test
    void jsonRejectsUnsupportedFormatVersion() throws IOException {
        Path file = tempDir.resolve("future.json");
        Files.writeString(file, "{\"formatVersion\":99,\"books\":[]}");
        IOException error = assertThrows(IOException.class, () -> json.load(file));
        assertTrue(error.getMessage().contains("version"));
    }

    @Test
    void binaryRejectsUnsupportedFormatVersion() throws IOException {
        Path file = tempDir.resolve("future.bin");
        binary.save(sampleData(), file);
        byte[] bytes = Files.readAllBytes(file);
        bytes[7] = 2; // The version is the second 32-bit integer in the header.
        Files.write(file, bytes);
        IOException error = assertThrows(IOException.class, () -> binary.load(file));
        assertTrue(error.getMessage().contains("version"));
    }

    @Test
    void jsonRejectsMalformedFile() throws IOException {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ this is not JSON");
        assertThrows(IOException.class, () -> json.load(file));
    }

    @Test
    void binaryRejectsTruncatedFile() throws IOException {
        Path file = tempDir.resolve("broken.bin");
        Files.write(file, new byte[]{0x4c, 0x49});
        assertThrows(IOException.class, () -> binary.load(file));
    }

    @Test
    void mapperRejectsInvalidCatalogVersion() {
        BookCatalogData future = new BookCatalogData(99, List.of());
        IOException error = assertThrows(IOException.class, () -> BookDataMapper.fromData(future));
        assertTrue(error.getMessage().contains("version"));
    }
}
