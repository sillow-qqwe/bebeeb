package ru.example.library.store;

/** Неизменяемое представление узла для визуализации; не раскрывает внутренние ссылки дерева. */
public record NodeSnapshot(String title, int parentIndex, boolean isLeftChild) { }
