package ru.example.library.io;

public class CsvParseException extends Exception {
    private final int lineNumber;
    private final CsvErrorCode errorCode;

    public CsvParseException(int lineNumber, CsvErrorCode errorCode, String details) {
        super("Строка " + lineNumber + ": " + errorCode.getDescription()
                + (details == null || details.isBlank() ? "" : " (" + details + ")"));
        this.lineNumber = lineNumber;
        this.errorCode = errorCode;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public CsvErrorCode getErrorCode() {
        return errorCode;
    }
}
