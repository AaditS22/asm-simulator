package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.frontend.util.ConfirmDialog;
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
import javafx.stage.StageStyle;

import java.awt.*;
import java.net.URI;

// DISCLAIMER: This class was largely written with the help of LLMs
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

    private HBox buildBrand() {
        Label appLabel = new Label("ASM SIM");
        appLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        Label sep = new Label("  /  ");
        sep.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12;");

        Label viewLabel = new Label("Home");
        viewLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        HBox brand = new HBox(0, appLabel, sep, viewLabel);
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setPrefHeight(40);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        HBox brand = buildBrand();

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

        Label closeBtn = new Label("✕");
        closeBtn.setPadding(new Insets(4, 10, 4, 10));
        String closeDefault =
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-cursor: hand;";
        String closeHover =
                "-fx-background-color: #C0392B;" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;";
        closeBtn.setStyle(closeDefault);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeHover));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(closeDefault));
        closeBtn.setOnMouseClicked(e -> {
            if (ConfirmDialog.show(getScene().getWindow(), "Are you sure you want to close the application?")) {
                ((Stage) getScene().getWindow()).close();
            }
        });

        bar.getChildren().addAll(brand, spacer, aboutBtn, closeBtn);
        return bar;
    }

    private void showAboutDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(getScene().getWindow());
        stage.initStyle(StageStyle.UNDECORATED);

        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 12, 0, 16));
        titleBar.setPrefHeight(36);
        titleBar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        Label titleBarLabel = new Label("About");
        titleBarLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Label closeX = getCloseX(stage);

        titleBar.getChildren().addAll(titleBarLabel, titleSpacer, closeX);

        // — Body —
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(36, 40, 36, 40));
        body.setStyle("-fx-background-color: " + BG_BASE + ";");

        Label appName = new Label("Version 1.0.0");
        appName.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setPrefWidth(200);
        divider.setMaxWidth(200);
        divider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        Label creator = new Label("Created by AaditS22");
        creator.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );

        Hyperlink githubLink = getGithubLink();

        body.getChildren().addAll(appName, divider, creator, githubLink);

        VBox root = new VBox(0, titleBar, body);
        root.setStyle(
                "-fx-background-color: " + BG_BASE + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;"
        );

        Scene scene = new Scene(root, 340, 280);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }

    private static Hyperlink getGithubLink() {
        Hyperlink githubLink = new Hyperlink("View project on GitHub →");
        githubLink.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + AMBER + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-underline: false;" +
                        "-fx-cursor: hand;"
        );
        githubLink.setOnMouseEntered(e -> githubLink.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-underline: true;" +
                        "-fx-cursor: hand;"
        ));
        githubLink.setOnMouseExited(e -> githubLink.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + AMBER + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-underline: false;" +
                        "-fx-cursor: hand;"
        ));
        githubLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/AaditS22/asm-simulator"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return githubLink;
    }

    private static Label getCloseX(Stage stage) {
        Label closeX = new Label("✕");
        closeX.setPadding(new Insets(4, 8, 4, 8));
        String xDefault =
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-cursor: hand;";
        String xHover =
                "-fx-background-color: #C0392B;" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;";
        closeX.setStyle(xDefault);
        closeX.setOnMouseEntered(e -> closeX.setStyle(xHover));
        closeX.setOnMouseExited(e -> closeX.setStyle(xDefault));
        closeX.setOnMouseClicked(e -> stage.close());
        return closeX;
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
                "Write and/or upload custom assembly code, then watch it run in the simulator!" +
                        " Visualize how the CPU's state is changing with each instruction, and get detailed" +
                        " descriptions of your code!"
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
        Label hint = new Label("DISCLAIMER: This is only a teaching tool and does not perfectly mimic real CPU " +
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
}