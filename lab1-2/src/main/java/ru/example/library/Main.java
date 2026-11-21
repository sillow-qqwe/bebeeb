package ru.example.library;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.example.library.ui.MainFrame;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        MainFrame root = new MainFrame(stage);
        stage.setTitle("Лабораторная работа №1 — Каталог книг");
        stage.setScene(new Scene(root, 1000, 560));
        stage.setMinWidth(820);
        stage.setMinHeight(420);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
