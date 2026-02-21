package io.github.AaditS22.asmsimulator.frontend;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.net.URI;

public class HomeView extends VBox {

    private static final String BG_BASE      = "#2B2B2B";
    private static final String BG_PANEL     = "#313335";
    private static final String BG_RAISED    = "#3C3F41";
    private static final String BORDER_SOFT  = "#424547";
    private static final String AMBER        = "#E8A845";
    private static final String BG_HOVER = "#4C5052";


    private static final String TEXT_PRIMARY = "#BBBBBB";
    private static final String TEXT_BRIGHT  = "#E8E8E8";
    private static final String TEXT_MUTED   = "#777777";

    private static final String RED_BG       = "#442726";
    private static final String RED_BORDER   = "#913B36";
    private static final String RED_TEXT     = "#FF9B94";

    private static final String SANS  = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO  = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    private static final String BTN_DEFAULT =
            "-fx-background-color: " + BG_RAISED + ";" +
                    "-fx-text-fill: " + AMBER + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12.5;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 28 10 28;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + AMBER + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_HOVER =
            "-fx-background-color: " + AMBER + ";" +
                    "-fx-text-fill: #1E1E1E;" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12.5;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 28 10 28;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + AMBER + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    public HomeView(Runnable onGetStarted) {
        setAlignment(Pos.CENTER);
        setSpacing(0);
        setStyle("-fx-background-color: " + BG_BASE + ";");

        VBox outer = new VBox(0);
        outer.setAlignment(Pos.CENTER);
        VBox.setVgrow(outer, Priority.ALWAYS);

        outer.getChildren().addAll(
                buildTopBar(),
                buildCenterContent(onGetStarted)
        );

        getChildren().add(outer);
        VBox.setVgrow(outer, Priority.ALWAYS);
    }

    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setPrefHeight(40);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        Label project = new Label("asm-simulator");
        project.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label aboutBtn = new Label("About");
        aboutBtn.setPadding(new Insets(4, 12, 4, 12));

        String aboutDefault =
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-cursor: hand;";

        String aboutHover =
                "-fx-background-color: " + BG_HOVER + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-cursor: hand;";

        aboutBtn.setStyle(aboutDefault);
        aboutBtn.setOnMouseEntered(e -> aboutBtn.setStyle(aboutHover));
        aboutBtn.setOnMouseExited(e -> aboutBtn.setStyle(aboutDefault));
        aboutBtn.setOnMouseClicked(e -> showAboutDialog());

        bar.getChildren().addAll(project, spacer, aboutBtn);
        return bar;
    }

    private void showAboutDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("About");

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + BG_BASE + ";");

        Label title = new Label("ASM SIM");
        title.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";"
        );

        Label creator = new Label("Created by AaditS22");
        creator.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );

        Hyperlink githubLink = new Hyperlink("View Project Code on GitHub");
        githubLink.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + AMBER + ";" +
                        "-fx-border-color: transparent;"
        );
        githubLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/AaditS22/asm-simulator"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(BTN_DEFAULT);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(BTN_HOVER));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(BTN_DEFAULT));
        closeBtn.setOnAction(e -> stage.close());

        VBox.setMargin(closeBtn, new Insets(10, 0, 0, 0));

        root.getChildren().addAll(title, creator, githubLink, closeBtn);

        Scene scene = new Scene(root, 320, 220);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }

    private StackPane buildCenterContent(Runnable onGetStarted) {
        StackPane wrapper = new StackPane();
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color: " + BG_BASE + ";");

        VBox contentContainer = new VBox(0);
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setMaxWidth(560);

        VBox mainPanel = buildMainPanel(onGetStarted);

        VBox disclaimer = buildDisclaimerPanel();

        contentContainer.getChildren().addAll(mainPanel, disclaimer);

        wrapper.getChildren().add(contentContainer);
        StackPane.setAlignment(contentContainer, Pos.CENTER);

        return wrapper;
    }

    private VBox buildMainPanel(Runnable onGetStarted) {
        VBox panel = new VBox(28);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(40, 44, 40, 44));
        panel.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0.1, 0, 6);"
        );

        panel.getChildren().addAll(
                buildTitleBlock(),
                buildSeparator(),
                buildDescription(),
                buildActionRow(onGetStarted)
        );

        animateIn(panel);
        return panel;
    }

    private VBox buildTitleBlock() {
        Label title = new Label("ASM Sim");
        title.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 48;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";"
        );

        Label sub = new Label("Assembly Simulator & Debugger");
        sub.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        VBox block = new VBox(6, title, sub);
        block.setAlignment(Pos.CENTER_LEFT);
        return block;
    }

    private Region buildSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: " + BORDER_SOFT + ";");
        return sep;
    }

    private Label buildDescription() {
        Label desc = new Label(
                "Write or upload assembly code, then run it instruction by instruction. " +
                        "Registers, the stack, flags, and memory update live so you can see exactly" +
                        " what your program is doing."
        );
        desc.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-line-spacing: 4;"
        );
        desc.setWrapText(true);
        return desc;
    }

    private VBox buildActionRow(Runnable onGetStarted) {
        Button btn = new Button("Open Editor");
        btn.setStyle(BTN_DEFAULT);
        btn.setOnMouseEntered(e -> btn.setStyle(BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(BTN_DEFAULT));
        btn.setOnAction(e -> onGetStarted.run());

        VBox actionColumn = new VBox(20, btn);
        actionColumn.setAlignment(Pos.CENTER_LEFT);
        return actionColumn;
    }

    private VBox buildDisclaimerPanel() {
        Label hint = new Label("DISCLAIMER: This is only a teaching tool and does not perfectly replicate real CPU " +
                "behaviour. It is made as a learning and experimentation tool, not to be used for production.");

        hint.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + RED_TEXT + ";" +
                        "-fx-line-spacing: 2;"
        );
        hint.setWrapText(true);
        hint.setMaxWidth(500);

        VBox warningBox = new VBox(hint);
        warningBox.setPadding(new Insets(12, 20, 12, 20));
        warningBox.setStyle(
                "-fx-background-color: " + RED_BG + ";" +
                        "-fx-border-color: " + RED_BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;"
        );

        // Slight margin to separate it from the main panel above
        VBox.setMargin(warningBox, new Insets(16, 0, 0, 0));

        return warningBox;
    }

    private void animateIn(VBox panel) {
        panel.setOpacity(0);
        panel.setTranslateY(16);

        FadeTransition fade = new FadeTransition(Duration.millis(500), panel);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(500), panel);
        slide.setFromY(16);
        slide.setToY(0);

        ParallelTransition intro = new ParallelTransition(fade, slide);
        intro.setDelay(Duration.millis(80));
        intro.play();
    }
}