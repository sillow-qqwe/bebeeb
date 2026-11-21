package ru.example.library.io;

public enum CsvErrorCode {
    WRONG_FIELD_COUNT("Неверное количество колонок в строке"),
    BAD_NUMBER("Ошибка преобразования числа"),
    BAD_BOOLEAN("Ошибка преобразования boolean (ожидается 0/1 или true/false)"),
    UNKNOWN_TYPE("Неизвестный тип сущности"),
    MISSING_FIELD("Обязательное поле пустое"),
    INVALID_DATA("Данные не прошли проверку"),
    MALFORMED_CSV("Нарушен синтаксис CSV");

    private final String description;

    CsvErrorCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
