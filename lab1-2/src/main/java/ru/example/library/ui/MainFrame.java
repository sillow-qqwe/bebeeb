package ru.example.library.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.example.library.io.CsvSerializer;
import ru.example.library.io.DefaultCsvSerializer;
import ru.example.library.model.AntiqueBook;
import ru.example.library.model.Book;
import ru.example.library.model.Textbook;
import ru.example.library.store.BstBookStore;
import ru.example.library.store.BstBenchmark;
import ru.example.library.store.BookGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Главное окно. BST — единственное основное хранилище; ObservableList является UI-проекцией. */
public class MainFrame extends BorderPane {
    private static final String BASE_TITLE = "Лабораторная работа №2 — Электронная библиотека";

    private final Stage stage;
    private final CsvSerializer serializer = new DefaultCsvSerializer();
    private final BstBookStore store = new BstBookStore();
    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final TableView<Book> table = new TableView<>(books);
    private final BstCanvasView treeView = new BstCanvasView(1100, 560);
    private final Button editButton = new Button("Изменить");
    private final Button deleteButton = new Button("Удалить");
    private final TextField searchField = new TextField();
    private boolean unsavedChanges;

    public MainFrame(Stage stage) {
        this.stage = stage;
        setPadding(new Insets(10));
        buildTable();
        buildLayout();
        installHandlers();
        refreshFromStore(List.of());
        updateWindowTitle();
    }

    private void buildTable() {
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        BookTableModel.configure(table);
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldBook, selected) -> updateButtonsState());
        table.setPrefHeight(230);
    }

    private void buildLayout() {
        Button addButton = new Button("Добавить");
        Button loadButton = new Button("Загрузить CSV");
        Button saveButton = new Button("Сохранить CSV");
        Button generateButton = new Button("Сгенерировать книги");
        Button benchmarkButton = new Button("Замерить BST");
        searchField.setPromptText("Название для поиска");
        searchField.setPrefColumnCount(16);
        Button searchButton = new Button("Найти");
        searchButton.setOnAction(event -> onSearch());
        searchField.setOnAction(event -> onSearch());
        ToolBar toolbar = new ToolBar(addButton, editButton, deleteButton,
                new Separator(), new Label("Поиск:"), searchField, searchButton,
                new Separator(), generateButton, benchmarkButton, loadButton, saveButton);

        TextField rangeFromField = new TextField();
        rangeFromField.setPromptText("От названия");
        rangeFromField.setPrefColumnCount(14);
        TextField rangeToField = new TextField();
        rangeToField.setPromptText("До названия");
        rangeToField.setPrefColumnCount(14);
        Button rangeButton = new Button("Найти в диапазоне");
        rangeButton.setOnAction(event ->
                onRangeSearch(rangeFromField.getText(), rangeToField.getText()));

        TextField lastCountField = new TextField();
        lastCountField.setPromptText("K");
        lastCountField.setPrefColumnCount(4);
        Button lastButton = new Button("Последние K по алфавиту");
        lastButton.setOnAction(event -> onLastK(lastCountField.getText()));

        HBox extraOperations = new HBox(8,
                new Label("Дополнительные операции:"),
                rangeFromField, rangeToField, rangeButton,
                new Separator(javafx.geometry.Orientation.VERTICAL), new Label("Количество:"), lastCountField, lastButton);
        extraOperations.setPadding(new Insets(6, 0, 6, 0));
        setTop(new VBox(4, toolbar, extraOperations));

        Label tableLabel = new Label("Каталог книг (" + store.size() + ")");
        VBox tableBox = new VBox(6, tableLabel, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label treeLabel = new Label("Двоичное дерево поиска — ключ: название книги");
        ScrollPane treeScroll = new ScrollPane(treeView);
        treeScroll.setPannable(true);
        treeScroll.setFitToHeight(false);
        treeScroll.setFitToWidth(false);
        VBox treeBox = new VBox(6, treeLabel, treeScroll);
        VBox.setVgrow(treeScroll, Priority.ALWAYS);

        SplitPane split = new SplitPane(tableBox, treeBox);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.42);
        setCenter(split);

        addButton.setOnAction(event -> onAdd());
        loadButton.setOnAction(event -> onLoad());
        saveButton.setOnAction(event -> saveWithChooser());
        generateButton.setOnAction(event -> onGenerate());
        benchmarkButton.setOnAction(event -> onBenchmark());
    }

    private void installHandlers() {
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        editButton.setOnAction(event -> onEdit());
        deleteButton.setOnAction(event -> onDelete());
        stage.setOnCloseRequest(event -> {
            if (!confirmSaveOrDiscardChanges()) event.consume();
        });
    }

    private void updateButtonsState() {
        Book selected = table.getSelectionModel().getSelectedItem();
        editButton.setDisable(selected == null || !(selected instanceof ru.example.library.model.Editable));
        deleteButton.setDisable(selected == null);
    }

    private void onAdd() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Обычная книга",
                List.of("Обычная книга", "Учебник"));
        dialog.setTitle("Добавление книги");
        dialog.setHeaderText("Выберите тип записи");
        dialog.setContentText("Тип:");
        dialog.showAndWait().ifPresent(choice -> showBookDialog(null, choice.equals("Учебник")));
    }

    private void onEdit() {
        Book selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!(selected instanceof ru.example.library.model.Editable)) {
            showAlert(Alert.AlertType.WARNING, "Редактирование запрещено", "Эта запись доступна только для чтения.");
            return;
        }
        showBookDialog(selected, selected instanceof Textbook);
    }

    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Поиск", "Введите название книги.");
            return;
        }
        Book found = store.findByTitle(query);
        List<String> visited = store.getLastVisitedTitles();
        refreshFromStore(visited);
        if (found == null) {
            table.getSelectionModel().clearSelection();
            showAlert(Alert.AlertType.INFORMATION, "Результат поиска", "Книга не найдена: " + query);
        } else {
            table.getSelectionModel().select(found);
            table.scrollTo(found);
        }
    }

    private void onRangeSearch(String from, String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Поиск по диапазону",
                    "Введите обе границы диапазона.");
            return;
        }
        try {
            List<Book> result = store.findInTitleRange(from, to);
            List<String> highlighted = result.stream().map(Book::getTitle).toList();
            refreshFromStore(highlighted);
            showAdditionalResults("Результат поиска по диапазону", result,
                    "Диапазон: «" + from.trim() + "» — «" + to.trim() + "»");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onLastK(String rawCount) {
        final int count;
        try {
            count = Integer.parseInt(rawCount == null ? "" : rawCount.trim());
            if (count < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Введите неотрицательное целое число K.");
            return;
        }
        List<Book> result = store.lastAlphabetically(count);
        List<String> highlighted = result.stream().map(Book::getTitle).toList();
        refreshFromStore(highlighted);
        showAdditionalResults("Последние K книг по алфавиту", result,
                "Запрошено: " + count + ", найдено: " + result.size()
                        + ". Порядок: от большего ключа к меньшему.");
    }

    private void showAdditionalResults(String title, List<Book> result, String details) {
        StringBuilder message = new StringBuilder(details)
                .append("\n\nНайдено книг: ").append(result.size());
        for (Book book : result) {
            message.append("\n• ").append(book.getTitle())
                    .append(" — ").append(book.getAuthor());
        }
        showAlert(Alert.AlertType.INFORMATION, title, message.toString());
    }

    private void onDelete() {
        Book selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить запись «" + selected.getTitle() + "»?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            store.removeByTitle(selected.getTitle());
            refreshFromStore(store.getLastVisitedTitles());
            markChanged();
        }
    }

    private void onLoad() {
        if (!confirmSaveOrDiscardChanges()) return;
        File file = createCsvChooser().showOpenDialog(stage);
        if (file == null) return;

        Task<LoadedCatalog> task = new Task<>() {
            @Override protected LoadedCatalog call() throws Exception {
                List<String> errors = new ArrayList<>();
                List<Book> loaded = serializer.load(file.toPath(), errors, fraction -> updateProgress(fraction, 1.0));
                BstBookStore replacement = new BstBookStore();
                int accepted = 0;
                for (Book book : loaded) {
                    if (isCancelled()) throw new java.util.concurrent.CancellationException();
                    try { replacement.add(book); accepted++; }
                    catch (IllegalArgumentException ex) {
                        errors.add("Не добавлена книга «" + book.getTitle() + "»: " + ex.getMessage());
                    }
                }
                updateProgress(1, 1);
                return new LoadedCatalog(replacement.inOrderBooks(), errors, accepted, loaded.size());
            }
        };
        showProgressTask("Загрузка CSV", "Чтение файла и построение дерева…", task, () -> {
            LoadedCatalog result = task.getValue();
            store.clear();
            for (Book book : result.books()) store.add(book);
            refreshFromStore(List.of());
            unsavedChanges = !result.errors().isEmpty();
            updateWindowTitle();
            String summary = "Добавлено в дерево: " + result.accepted() + " из " + result.total();
            if (result.errors().isEmpty()) showAlert(Alert.AlertType.INFORMATION, "Загрузка завершена", summary);
            else showAlert(Alert.AlertType.WARNING, "Загрузка завершена с замечаниями",
                    summary + "\n\n" + String.join("\n", result.errors()));
        });
    }

    private void onGenerate() {
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Генерация каталога");
        dialog.setHeaderText("Создание тестовых книг");
        dialog.setContentText("Количество книг (1–100000):");
        Optional<String> answer = dialog.showAndWait();
        if (answer.isEmpty()) return;
        final int count;
        try {
            count = Integer.parseInt(answer.get().trim());
            if (count < 1 || count > 100_000) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Введите целое число от 1 до 100000.");
            return;
        }

        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() {
                return BookGenerator.generate(count, completed -> {
                    if (isCancelled()) throw new java.util.concurrent.CancellationException();
                    updateProgress(completed, count);
                    updateMessage("Создано " + completed + " из " + count);
                });
            }
        };
        showProgressTask("Генерация книг", "Подготовка записей…", task, () -> {
            int added = 0;
            for (Book book : task.getValue()) {
                try { store.add(book); added++; }
                catch (IllegalArgumentException ignored) { /* конфликт названия исключён генератором, страховка */ }
            }
            refreshFromStore(List.of());
            if (added > 0) markChanged();
            showAlert(Alert.AlertType.INFORMATION, "Генерация завершена",
                    "Добавлено книг: " + added + "\nВсего в каталоге: " + store.size());
        });
    }

    private void onBenchmark() {
        Task<String> task = new Task<>() {
            @Override protected String call() {
                updateMessage("Выполняются замеры BST и TreeMap…");
                return BstBenchmark.run();
            }
        };
        showProgressTask("Замеры производительности", "Подготовка замеров…", task, () ->
                showAlert(Alert.AlertType.INFORMATION, "Результаты замеров", task.getValue()));
    }

    private <T> void showProgressTask(String title, String message, Task<T> task, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(message);
        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(message);
        VBox content = new VBox(10, status, progress);
        content.setPadding(new Insets(12));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());
        dialog.setOnCloseRequest(event -> {
            if (task.isRunning()) task.cancel();
        });
        task.setOnSucceeded(event -> {
            dialog.close();
            try { onSuccess.run(); }
            catch (RuntimeException ex) { showError("Операция завершилась с ошибкой: " + ex.getMessage()); }
        });
        task.setOnFailed(event -> {
            dialog.close();
            Throwable ex = task.getException();
            showError("Не удалось выполнить операцию: " + (ex == null ? "неизвестная ошибка" : ex.getMessage()));
        });
        task.setOnCancelled(event -> dialog.close());
        Thread worker = new Thread(task, "library-background-task");
        worker.setDaemon(true);
        worker.start();
        dialog.show();
    }

    private record LoadedCatalog(List<Book> books, List<String> errors, int accepted, int total) { }

    private boolean saveWithChooser() {
        File selected = createCsvChooser().showSaveDialog(stage);
        if (selected == null) return false;
        Path path = ensureCsvExtension(selected.toPath());
        if (Files.exists(path)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Файл уже существует. Перезаписать его?",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("Подтверждение перезаписи");
            alert.setHeaderText(null);
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return false;
        }
        try {
            serializer.save(path, store.inOrderBooks());
            unsavedChanges = false;
            updateWindowTitle();
            showAlert(Alert.AlertType.INFORMATION, "Сохранение завершено",
                    "Сохранено книг: " + store.size() + "\nФайл: " + path);
            return true;
        } catch (IOException | RuntimeException ex) {
            showError("Не удалось сохранить файл:\n" + ex.getMessage());
            return false;
        }
    }

    private FileChooser createCsvChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите CSV-файл");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файлы (*.csv)", "*.csv"));
        return chooser;
    }

    private Path ensureCsvExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") ? path : Path.of(path + ".csv");
    }

    private boolean confirmSaveOrDiscardChanges() {
        if (!unsavedChanges) return true;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Несохранённые изменения");
        alert.setHeaderText("Есть несохранённые изменения.");
        alert.setContentText("Сохранить их перед продолжением?");
        ButtonType save = new ButtonType("Сохранить");
        ButtonType discard = new ButtonType("Не сохранять");
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        ButtonType answer = alert.showAndWait().orElse(cancel);
        if (answer == save) return saveWithChooser();
        return answer == discard;
    }

    private void showBookDialog(Book existing, boolean textbook) {
        Dialog<BookFormResult> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Добавить книгу" : "Редактировать книгу");
        dialog.setHeaderText(textbook ? "Данные учебника" : "Данные книги");
        ButtonType accept = new ButtonType(existing == null ? "Добавить" : "Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accept, ButtonType.CANCEL);
        TextField isbn = field(existing == null ? "" : existing.getIsbn());
        TextField title = field(existing == null ? "" : existing.getTitle());
        TextField author = field(existing == null ? "" : existing.getAuthor());
        TextField year = field(existing == null ? "" : Integer.toString(existing.getYear()));
        TextField genre = field(existing == null ? "" : existing.getGenre());
        CheckBox onHands = new CheckBox("На руках");
        if (existing != null) onHands.setSelected(existing.isOnHands());
        TextField course = field(existing instanceof Textbook tb ? Integer.toString(tb.getCourse()) : "");
        TextField printRun = field(existing instanceof Textbook tb ? Integer.toString(tb.getPrintRun()) : "");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(10));
        int row = 0;
        addField(grid, row++, "ISBN:", isbn); addField(grid, row++, "Название:", title);
        addField(grid, row++, "Автор:", author); addField(grid, row++, "Год:", year);
        addField(grid, row++, "Жанр:", genre); addField(grid, row++, "Статус:", onHands);
        if (textbook) { addField(grid, row++, "Курс (1–5):", course); addField(grid, row, "Тираж:", printRun); }
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == accept ? new BookFormResult(isbn.getText().trim(),
                title.getText().trim(), author.getText().trim(), year.getText().trim(), genre.getText().trim(),
                onHands.isSelected(), course.getText().trim(), printRun.getText().trim()) : null);
        dialog.showAndWait().ifPresent(form -> applyForm(existing, textbook, form));
    }

    private void applyForm(Book existing, boolean textbook, BookFormResult form) {
        try {
            int newYear = Integer.parseInt(form.year());
            int newCourse = textbook ? Integer.parseInt(form.course()) : 0;
            int newPrintRun = textbook ? Integer.parseInt(form.printRun()) : 0;
            Book candidate = textbook ? new Textbook(form.isbn(), form.title(), form.author(), newYear,
                    form.genre(), form.onHands(), newCourse, newPrintRun)
                    : new Book(form.isbn(), form.title(), form.author(), newYear, form.genre(), form.onHands());
            List<String> errors = candidate.validate();
            if (!errors.isEmpty()) { showError(String.join("\n", errors)); return; }
            if (hasDuplicateIsbn(form.isbn(), existing)) {
                showAlert(Alert.AlertType.WARNING, "Дублирующийся ISBN", "Книга с таким ISBN уже существует."); return;
            }
            if (existing == null) {
                store.add(candidate);
            } else {
                String oldIsbn = existing.getIsbn();
                String oldTitle = existing.getTitle();
                String oldAuthor = existing.getAuthor();
                int oldYear = existing.getYear();
                String oldGenre = existing.getGenre();
                boolean oldOnHands = existing.isOnHands();
                int oldCourse = existing instanceof Textbook tb ? tb.getCourse() : 0;
                int oldPrintRun = existing instanceof Textbook tb ? tb.getPrintRun() : 0;
                store.removeByTitle(oldTitle);
                existing.setIsbn(form.isbn()); existing.setTitle(form.title()); existing.setAuthor(form.author());
                existing.setYear(newYear); existing.setGenre(form.genre()); existing.setOnHands(form.onHands());
                if (existing instanceof Textbook tb) { tb.setCourse(newCourse); tb.setPrintRun(newPrintRun); }
                try { store.add(existing); }
                catch (IllegalArgumentException ex) {
                    // При конфликте ключа восстанавливаем объект целиком и возвращаем его в дерево.
                    existing.setIsbn(oldIsbn); existing.setTitle(oldTitle); existing.setAuthor(oldAuthor);
                    existing.setYear(oldYear); existing.setGenre(oldGenre); existing.setOnHands(oldOnHands);
                    if (existing instanceof Textbook tb) { tb.setCourse(oldCourse); tb.setPrintRun(oldPrintRun); }
                    store.add(existing);
                    throw ex;
                }
            }
            refreshFromStore(store.getLastVisitedTitles());
            markChanged();
        } catch (NumberFormatException ex) {
            showError(textbook ? "Год, курс и тираж должны быть целыми числами." : "Год должен быть целым числом.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private boolean hasDuplicateIsbn(String isbn, Book ignored) {
        String normalized = normalizeIsbn(isbn);
        return store.inOrderBooks().stream().filter(book -> book != ignored)
                .anyMatch(book -> normalizeIsbn(book.getIsbn()).equals(normalized));
    }

    private String normalizeIsbn(String isbn) { return isbn == null ? "" : isbn.trim().toUpperCase(Locale.ROOT); }
    private TextField field(String value) { TextField field = new TextField(value); field.setPrefColumnCount(24); return field; }
    private void addField(GridPane grid, int row, String label, javafx.scene.Node control) {
        grid.add(new Label(label), 0, row); grid.add(control, 1, row);
    }

    private void refreshFromStore(List<String> highlighted) {
        books.setAll(store.inOrderBooks());
        treeView.render(store.snapshot(), highlighted);
        updateButtonsState();
        // Обновляем заголовок счётчика без создания отдельной модели данных.
    }

    private void markChanged() { unsavedChanges = true; updateWindowTitle(); }
    private void updateWindowTitle() { stage.setTitle(BASE_TITLE + " — книг: " + store.size() + (unsavedChanges ? " *" : "")); }
    private void showError(String message) { showAlert(Alert.AlertType.ERROR, "Ошибка", message); }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
    private record BookFormResult(String isbn, String title, String author, String year, String genre,
                                  boolean onHands, String course, String printRun) { }
}
