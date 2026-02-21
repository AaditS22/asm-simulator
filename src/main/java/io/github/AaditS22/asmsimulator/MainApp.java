package io.github.AaditS22.asmsimulator;

import io.github.AaditS22.asmsimulator.frontend.HomeView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        HomeView home = new HomeView(() -> {
        });

        Scene scene = new Scene(home, 900, 600);
        stage.setScene(scene);
        stage.setTitle("ASM SIM");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}