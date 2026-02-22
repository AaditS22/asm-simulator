package io.github.AaditS22.asmsimulator.frontend.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ErrorDialog {

    private static final String BG_BASE     = "#2B2B2B";
    private static final String BORDER_SOFT = "#424547";
    private static final String TEXT_BRIGHT = "#E8E8E8";
    private static final String TEXT_MUTED  = "#777777";
    private static final String RED_TEXT    = "#E06C75";
    private static final String RED_BG      = "#2D1A1A";
    private static final String RED_BORDER  = "#5C2D2D";
    private static final String BG_RAISED   = "#3C3F41";
    private static final String BG_HOVER    = "#4C5052";
    private static final String SANS        = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";

    public static void showParseError(Window owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.TRANSPARENT);

        Label title = new Label("Parse Error");
        title.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 14;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + RED_TEXT + ";"
        );

        Label body = new Label(
                "There was an error parsing your code. " +
                        "Check the terminal for the full error message to see what went wrong " +
                        "and go back to the editor to fix it."
        );
        body.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-text-alignment: center;" +
                        "-fx-line-spacing: 2;"
        );
        body.setWrapText(true);
        body.setMaxWidth(300);
        body.setAlignment(Pos.CENTER);

        Button okBtn = new Button("OK");
        okBtn.setPrefWidth(100);
        okBtn.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-padding: 6 18 6 18;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-cursor: hand;"
        );
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                "-fx-background-color: " + BG_HOVER + ";" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-padding: 6 18 6 18;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-cursor: hand;"
        ));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-padding: 6 18 6 18;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-cursor: hand;"
        ));
        okBtn.setOnAction(e -> dialog.close());

        VBox root = new VBox(16, title, body, okBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setMaxWidth(364);
        root.setStyle(
                "-fx-background-color: " + BG_BASE + ";" +
                        "-fx-border-color: " + RED_BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.centerOnScreen();
        dialog.showAndWait();
    }
}