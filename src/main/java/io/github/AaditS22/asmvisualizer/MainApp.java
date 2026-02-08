package io.github.AaditS22.asmvisualizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 300, 250);
        stage.setScene(scene);
        stage.setTitle("Assembly Visualizer");
        stage.show();
    }
}

