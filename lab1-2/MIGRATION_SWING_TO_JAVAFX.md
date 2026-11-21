# Переход с Swing на JavaFX

## Что изменено
- Точка входа `Main` теперь наследуется от `javafx.application.Application`.
- Главное окно `MainFrame` реализовано как JavaFX `BorderPane`.
- Таблица каталога построена на `TableView<Book>`; настройка колонок находится в `BookTableModel`.
- Диалоги добавления/редактирования, подтверждения, уведомления и выбор CSV-файла переведены на JavaFX.
- В `build.gradle.kts` подключён OpenJFX 21.0.2 (`javafx.controls`).

## Что намеренно сохранено
- Пакеты `model` и `io`, модели `Book`, `Textbook`, `AntiqueBook`, интерфейс `Editable`.
- CSV-формат и реализация `DefaultCsvSerializer`.
- Правило редактирования: обычная книга и учебник изменяются через сеттеры; `AntiqueBook` остаётся read-only.
- Проверка корректности полей, уникальности ISBN и предупреждение о несохранённых изменениях.

## Запуск
Требуется JDK 21 и доступ Gradle к Maven Central при первой сборке:

```bash
./gradlew test
./gradlew run
```

В IntelliJ IDEA следует открыть проект как Gradle-проект и дождаться синхронизации зависимостей.
