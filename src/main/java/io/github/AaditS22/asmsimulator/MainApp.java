package io.github.AaditS22.asmsimulator;

import io.github.AaditS22.asmsimulator.frontend.EditorView;
import io.github.AaditS22.asmsimulator.frontend.HomeView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private HomeView homeView;
    private EditorView editorView;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ASM SIM");

        this.editorView = new EditorView(() -> showHome(stage));
        this.homeView = new HomeView(() -> showEditor(stage));

        Scene scene = new Scene(homeView, 1280, 800);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private void showHome(Stage stage) {
        stage.getScene().setRoot(homeView);
    }

    private void showEditor(Stage stage) {
        stage.getScene().setRoot(editorView);
    }
}