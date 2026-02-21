package io.github.AaditS22.asmsimulator.frontend.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

// DISCLAIMER: This class was largely written with the help of LLMs
public class ConfirmDialog {

    private static final String BG_BASE     = "#2B2B2B";
    private static final String BG_PANEL    = "#313335";
    private static final String BG_RAISED   = "#3C3F41";
    private static final String BG_HOVER    = "#4C5052";
    private static final String BORDER_SOFT = "#424547";
    private static final String TEXT_BRIGHT = "#E8E8E8";
    private static final String TEXT_MUTED  = "#777777";
    private static final String AMBER       = "#E8A845";
    private static final String SANS        = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";

    public static boolean show(Window owner, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        final boolean[] confirmed = {false};

        Label msg = new Label(message);
        msg.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-text-alignment: center;"
        );
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setAlignment(Pos.CENTER);

        Button yesBtn = new Button("Yes, close");
        yesBtn.setPrefWidth(110);
        styleBtn(yesBtn, "#C0392B", "#E74C3C", "white");
        yesBtn.setOnAction(e -> { confirmed[0] = true; dialog.close(); });

        Button noBtn = new Button("Cancel");
        noBtn.setPrefWidth(110);
        styleBtn(noBtn, BG_RAISED, BG_HOVER, AMBER);
        noBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(12, noBtn, yesBtn);
        buttons.setAlignment(Pos.CENTER);

        Region topBorder = new Region();
        topBorder.setPrefHeight(1);
        topBorder.setMaxWidth(Double.MAX_VALUE);
        topBorder.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle(
                "-fx-background-color: " + BG_BASE + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;"
        );
        root.getChildren().addAll(msg, buttons);

        Scene scene = new Scene(root, 340, 140);
        dialog.setScene(scene);
        dialog.showAndWait();
        return confirmed[0];
    }

    private static void styleBtn(Button btn, String bg, String bgHover, String textColor) {
        String base =
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;";
        String hover =
                "-fx-background-color: " + bgHover + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}