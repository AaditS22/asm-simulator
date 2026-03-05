package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.backend.Simulator;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.util.StepResult;
import io.github.AaditS22.asmsimulator.frontend.util.AsmHighlighter;
import io.github.AaditS22.asmsimulator.frontend.util.ConfirmDialog;
import io.github.AaditS22.asmsimulator.frontend.util.ErrorDialog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.*;

// DISCLAIMER: This class was largely written with the help of LLMs
public class SimulatorView extends VBox {

    private static final String BG_BASE       = "#2B2B2B";
    private static final String BG_EDITOR     = "#1E1F22";
    private static final String BG_GUTTER     = "#252527";
    private static final String BG_PANEL      = "#313335";
    private static final String BG_RAISED     = "#3C3F41";
    private static final String BG_HOVER      = "#4C5052";
    private static final String BORDER_SOFT   = "#424547";
    private static final String AMBER         = "#E8A845";
    private static final String TEXT_PRIMARY  = "#BBBBBB";
    private static final String TEXT_BRIGHT   = "#E8E8E8";
    private static final String TEXT_MUTED    = "#777777";
    private static final String GUTTER_TEXT   = "#555555";
    private static final String GREEN         = "#6A8759";
    private static final String BLUE          = "#56A6E8";
    private static final String RED_BG        = "#442726";
    private static final String RED_BORDER    = "#913B36";
    private static final String RED_TEXT      = "#FF9B94";

    private static final String ACTIVE_LINE_BG = "#16537E";

    private static final String TERMINAL_BG    = "#141618";
    private static final String TERMINAL_GREEN = "#4EC94E";
    private static final String TERMINAL_WHITE = "#D4D4D4";

    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    private static final String BTN_PRIMARY =
            "-fx-background-color: " + AMBER + ";" +
                    "-fx-text-fill: #1E1E1E;" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-background-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_PRIMARY_HOVER =
            "-fx-background-color: #F5BE6A;" +
                    "-fx-text-fill: #1E1E1E;" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-background-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String DARK_SCROLL_CSS;
    static {
        String css =
                ".scroll-bar:horizontal,.scroll-bar:vertical{" +
                        "-fx-background-color:#141618;-fx-background-radius:0;}" +
                        ".scroll-bar:horizontal .thumb,.scroll-bar:vertical .thumb{" +
                        "-fx-background-color:#3C3F41;-fx-background-radius:3;}" +
                        ".scroll-bar:horizontal .thumb:hover,.scroll-bar:vertical .thumb:hover{" +
                        "-fx-background-color:#4C5052;-fx-background-radius:3;}" +
                        ".scroll-bar .increment-button,.scroll-bar .decrement-button{" +
                        "-fx-background-color:transparent;-fx-border-color:transparent;-fx-padding:0 0 0 0;}" +
                        ".scroll-bar .increment-arrow,.scroll-bar .decrement-arrow{" +
                        "-fx-shape:' ';-fx-padding:0;}" +
                        ".corner{-fx-background-color:#141618;}";
        DARK_SCROLL_CSS = "data:text/css;base64," +
                Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    private static final String BTN_SECONDARY =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + BORDER_SOFT + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_SECONDARY_HOVER =
            "-fx-background-color: " + BG_RAISED + ";" +
                    "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + BORDER_SOFT + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final Font TERMINAL_FONT;
    static {
        Font jb = Font.font("JetBrains Mono", 12);
        if (jb.getFamily().equals("JetBrains Mono")) {
            TERMINAL_FONT = jb;
        } else {
            Font consolas = Font.font("Consolas", 12);
            if (consolas.getFamily().equals("Consolas")) {
                TERMINAL_FONT = consolas;
            } else {
                TERMINAL_FONT = Font.font("Monospaced", 12);
            }
        }
    }

    private List<HBox> codeRows = new ArrayList<>();
    private ScrollPane codeScrollPane;
    private TextFlow terminalFlow;
    private ScrollPane terminalScroll;
    private Label stepCounterLabel;
    private TextField terminalInputField;
    private VBox terminalPane;
    private Label instructionDescLabel;
    private Label instructionTagLabel;
    private boolean terminalInputActive = false;

    // Execution Controls
    private Button stepBtn;
    private Button restartBtn;
    private Button autoPlayBtn;
    private Button runToEndBtn;
    private Slider speedSlider;

    // Concurrency states
    private volatile boolean isAutoPlaying = false;
    private volatile boolean isRunningToEnd = false;
    private Thread executionThread;

    private final Simulator simulator = new Simulator();
    private boolean parseSuccess = false;
    private final StringBuilder terminalContent = new StringBuilder();
    private List<Integer> instructionLineMap = new ArrayList<>();

    private final Runnable onBack;
    private final String assemblyCode;
    private int currentStep = 0;

    private StackView stackView;
    private RegistersView registersView;
    private FlagsView flagsView;
    private MemoryView memoryView;

    public SimulatorView(Runnable onBack, String assemblyCode) {
        this.onBack = onBack;
        this.assemblyCode = assemblyCode == null ? "" : assemblyCode;
        buildUI();
    }

    private void buildUI() {
        setStyle("-fx-background-color: " + BG_BASE + ";");
        VBox.setVgrow(this, Priority.ALWAYS);

        HBox titleBar    = buildTitleBar();
        HBox controlBar  = buildControlBar();
        HBox mainContent = buildMainContent();
        VBox terminal    = buildTerminal();

        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // ── Terminal Resizer Handle ──
        HBox resizer = new HBox();
        resizer.setAlignment(Pos.CENTER);
        resizer.setMinHeight(8);
        resizer.setPrefHeight(8);
        resizer.setMaxHeight(8);
        resizer.setStyle("-fx-background-color: " + BG_BASE + "; -fx-cursor: v-resize;");

        Region handle = new Region();
        handle.setPrefWidth(40);
        handle.setPrefHeight(3);
        handle.setMaxHeight(3);
        handle.setStyle("-fx-background-color: " + BORDER_SOFT + "; -fx-background-radius: 2;");
        resizer.getChildren().add(handle);

        resizer.setOnMouseEntered(e -> handle.setStyle("-fx-background-color: " + AMBER +
                "; -fx-background-radius: 2;"));
        resizer.setOnMouseExited(e -> handle.setStyle("-fx-background-color: " + BORDER_SOFT +
                "; -fx-background-radius: 2;"));

        resizer.setOnMouseDragged(e -> {
            double newHeight = this.getHeight() - e.getSceneY();
            if (newHeight < 145) newHeight = 145;

            double maxAllowed = this.getHeight() - 250;
            if (newHeight > maxAllowed) newHeight = maxAllowed;

            terminal.setPrefHeight(newHeight);
            terminal.setMinHeight(newHeight);
        });

        getChildren().addAll(titleBar, controlBar, mainContent, resizer, terminal);
        initSimulator();
    }

    // ── Title Bar ──────────────────────────────────────────────────────────────

    private HBox buildTitleBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        VBox.setVgrow(bar, Priority.NEVER);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";"
        );

        HBox brandBox = buildBrand();
        HBox.setMargin(brandBox, new Insets(0, 0, 0, 16));

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        HBox windowBtns = buildWindowButtons();
        HBox.setMargin(windowBtns, new Insets(0, 12, 0, 0));

        bar.getChildren().addAll(brandBox, leftSpacer, windowBtns);
        return bar;
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

        Label viewLabel = new Label("Simulator");
        viewLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        HBox brand = new HBox(0, appLabel, sep, viewLabel);
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private HBox buildWindowButtons() {
        Label backBtn = new Label("← Editor");
        backBtn.setPadding(new Insets(4, 12, 4, 12));
        String backDefault =
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-cursor: hand;";
        String backHover =
                "-fx-background-color: " + BG_HOVER + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                        "-fx-cursor: hand;";
        backBtn.setStyle(backDefault);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backHover));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(backDefault));
        backBtn.setOnMouseClicked(e -> {
            stopAutomatedExecution();
            onBack.run();
        });

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
                stopAutomatedExecution();
                ((Stage) getScene().getWindow()).close();
            }
        });

        HBox windowBtns = new HBox(6, backBtn, closeBtn);
        windowBtns.setAlignment(Pos.CENTER_RIGHT);
        return windowBtns;
    }

    // ── Execution Control Bar ─────────────────────────────────────────────────

    private HBox buildControlBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle(
                "-fx-background-color: " + BG_EDITOR + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        restartBtn = new Button("⟳ Restart");
        restartBtn.setStyle(BTN_SECONDARY);
        restartBtn.setOnMouseEntered(e -> restartBtn.setStyle(BTN_SECONDARY_HOVER));
        restartBtn.setOnMouseExited(e -> restartBtn.setStyle(BTN_SECONDARY));
        restartBtn.setOnAction(e -> handleReset());

        stepBtn = new Button("Step ▶");
        stepBtn.setStyle(BTN_PRIMARY);
        stepBtn.setOnMouseEntered(e -> {
            if (!stepBtn.isDisabled()) stepBtn.setStyle(BTN_PRIMARY_HOVER); });
        stepBtn.setOnMouseExited(e -> {
            if (!stepBtn.isDisabled()) stepBtn.setStyle(BTN_PRIMARY); });
        stepBtn.setOnAction(e -> handleStep());

        autoPlayBtn = new Button("Autoplay ▶▶");
        autoPlayBtn.setStyle(BTN_SECONDARY);
        autoPlayBtn.setOnMouseEntered(e -> {
            if (!autoPlayBtn.isDisabled()) autoPlayBtn.setStyle(BTN_SECONDARY_HOVER); });
        autoPlayBtn.setOnMouseExited(e -> {
            if (!autoPlayBtn.isDisabled()) autoPlayBtn.setStyle(BTN_SECONDARY); });
        autoPlayBtn.setOnAction(e -> toggleAutoPlay());

        Label speedLabel = new Label("Delay:");
        speedLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-family: " + SANS + ";");

        speedSlider = new Slider(10, 1000, 250);
        speedSlider.setPrefWidth(100);

        HBox autoPlayBox = new HBox(8, autoPlayBtn, speedLabel, speedSlider);
        autoPlayBox.setAlignment(Pos.CENTER_LEFT);

        runToEndBtn = new Button("Run to End ⏭");
        runToEndBtn.setStyle(BTN_SECONDARY);
        runToEndBtn.setOnMouseEntered(e -> {
            if (!runToEndBtn.isDisabled()) runToEndBtn.setStyle(BTN_SECONDARY_HOVER); });
        runToEndBtn.setOnMouseExited(e -> {
            if (!runToEndBtn.isDisabled()) runToEndBtn.setStyle(BTN_SECONDARY); });
        runToEndBtn.setOnAction(e -> handleRunToEnd());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        stepCounterLabel = new Label("step  0");
        stepCounterLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 0 4 0 4;"
        );

        bar.getChildren().addAll(restartBtn, stepBtn, autoPlayBox, runToEndBtn, spacer, stepCounterLabel);
        return bar;
    }

    // ── Main Content ──────────────────────────────────────────────────────────

    private HBox buildMainContent() {
        HBox content = new HBox(0);
        content.setStyle("-fx-background-color: " + BG_BASE + ";");
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox codePane = buildCodePane();
        codePane.setMinWidth(330);
        codePane.setPrefWidth(420);
        codePane.setMaxWidth(560);

        Region vertDivider = new Region();
        vertDivider.setPrefWidth(1);
        vertDivider.setMaxHeight(Double.MAX_VALUE);
        vertDivider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        VBox vizArea = buildVisualizationArea();
        HBox.setHgrow(vizArea, Priority.ALWAYS);

        content.getChildren().addAll(codePane, vertDivider, vizArea);
        return content;
    }

    // ── Code Pane (Left) ──────────────────────────────────────────────────────

    private VBox buildCodePane() {
        VBox pane = new VBox(0);
        pane.setStyle("-fx-background-color: " + BG_EDITOR + ";");

        HBox header = buildCodePaneHeader();
        codeScrollPane = buildCodeView();
        VBox.setVgrow(codeScrollPane, Priority.ALWAYS);

        pane.getChildren().addAll(header, codeScrollPane, buildInstructionDescPane());
        return pane;
    }

    private HBox buildCodePaneHeader() {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 14, 0, 14));
        header.setPrefHeight(36);
        header.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        Label title = new Label("Source Code");
        title.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("READ ONLY / Go back to the editor to edit code");
        badge.setPadding(new Insets(2, 7, 2, 7));
        badge.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-background-radius: 3;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-letter-spacing: 0.5;"
        );

        header.getChildren().addAll(title, spacer, badge);
        return header;
    }

    private ScrollPane buildCodeView() {
        VBox linesBox = new VBox(0);
        linesBox.setStyle("-fx-background-color: " + BG_EDITOR + ";");

        String[] lines = assemblyCode.split("\\R", -1);
        codeRows.clear();
        for (int i = 0; i < lines.length; i++) {
            HBox row = buildCodeRow(i + 1, lines[i]);
            codeRows.add(row);
            linesBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(linesBox);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: " + BG_EDITOR + ";" +
                        "-fx-background-color: " + BG_EDITOR + ";" +
                        "-fx-border-color: transparent;"
        );
        scroll.getStylesheets().add(DARK_SCROLL_CSS);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private HBox buildCodeRow(int lineNumber, String lineText) {
        Label gutterLabel = new Label(String.valueOf(lineNumber));
        gutterLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + GUTTER_TEXT + ";" +
                        "-fx-padding: 1 8 1 8;" +
                        "-fx-background-color: " + BG_GUTTER + ";" +
                        "-fx-pref-width: 48;" +
                        "-fx-min-width: 48;" +
                        "-fx-alignment: CENTER_RIGHT;"
        );

        TextFlow codeFlow = AsmHighlighter.buildHighlightedLine(lineText);
        codeFlow.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 1 14 1 10;"
        );

        HBox row = new HBox(0, gutterLabel, codeFlow);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(22);
        row.setPrefHeight(22);
        row.setStyle("-fx-background-color: " + BG_EDITOR + ";");
        return row;
    }

    // ── Instruction Description Pane ──────────────────────────────────────────

    private VBox buildInstructionDescPane() {
        VBox pane = new VBox(0);
        pane.setMinHeight(110);
        pane.setPrefHeight(120);
        pane.setMaxHeight(140);
        pane.setStyle(
                "-fx-background-color: #1C1E21;" +
                        "-fx-border-color: " + AMBER + " transparent transparent transparent;" +
                        "-fx-border-width: 2 0 0 0;"
        );

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 14, 6, 14));

        instructionTagLabel = new Label("AWAITING EXECUTION");
        instructionTagLabel.setPadding(new Insets(2, 8, 2, 8));
        instructionTagLabel.setStyle(
                "-fx-background-color: #2E2508;" +
                        "-fx-background-radius: 3;" +
                        "-fx-border-color: #5A4010;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 3;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";" +
                        "-fx-letter-spacing: 0.6;"
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label icon = new Label("⚙");
        icon.setStyle("-fx-font-size: 11; -fx-text-fill: #3A3010;");

        headerRow.getChildren().addAll(instructionTagLabel, headerSpacer, icon);

        instructionDescLabel = new Label("A simple explanation of each executed instruction " +
                "will be visible here");
        instructionDescLabel.setWrapText(true);
        instructionDescLabel.setMaxWidth(Double.MAX_VALUE);
        instructionDescLabel.setPadding(new Insets(0, 16, 10, 16));
        instructionDescLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: #555759;" +
                        "-fx-line-spacing: 2;"
        );

        pane.getChildren().addAll(headerRow, instructionDescLabel);
        return pane;
    }

    // ── Visualization Area (Right) ────────────────────────────────────────────

    private VBox buildVisualizationArea() {
        VBox area = new VBox(0);
        area.setStyle("-fx-background-color: " + BG_BASE + ";");
        area.setPadding(new Insets(14, 16, 14, 16));
        VBox.setVgrow(area, Priority.ALWAYS);

        HBox mainRow = buildMainPaneRow();
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        area.getChildren().add(mainRow);
        return area;
    }

    private HBox buildMainPaneRow() {
        VBox stackPane = buildPane("Stack", buildStackContent(), true);
        stackPane.setMinWidth(260);
        stackPane.setPrefWidth(280);
        stackPane.setMaxWidth(320);

        VBox rightColumn = buildRightColumn();
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox row = new HBox(12, stackPane, rightColumn);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(row, Priority.ALWAYS);
        return row;
    }

    private VBox buildRightColumn() {
        HBox topRow = buildTopPaneRow();
        topRow.setMinHeight(310);
        topRow.setPrefHeight(350);
        topRow.setMaxHeight(350);

        Region rowGap = new Region();
        rowGap.setPrefHeight(12);
        rowGap.setMinHeight(12);
        rowGap.setMaxHeight(12);

        VBox registersPane = buildPane("Registers", buildRegistersContent(), true);
        VBox.setVgrow(registersPane, Priority.ALWAYS);

        VBox col = new VBox(0, topRow, rowGap, registersPane);
        VBox.setVgrow(col, Priority.ALWAYS);
        return col;
    }

    private HBox buildTopPaneRow() {
        VBox memoryPane = buildPane("Memory", buildMemoryContent(), true);
        VBox flagsPane  = buildPane("Flags",  buildFlagsContent(),  false);

        flagsPane.setMinWidth(158);
        flagsPane.setPrefWidth(162);
        flagsPane.setMaxWidth(162);

        HBox.setHgrow(memoryPane, Priority.ALWAYS);

        HBox row = new HBox(12, memoryPane, flagsPane);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private VBox buildPane(String title, Node content, boolean growContent) {
        VBox pane = new VBox(0);
        pane.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 16, 0.08, 0, 5);"
        );
        VBox.setVgrow(pane, Priority.ALWAYS);

        HBox header = buildPaneHeader(title);

        Region headerDivider = new Region();
        headerDivider.setPrefHeight(1);
        headerDivider.setMaxWidth(Double.MAX_VALUE);
        headerDivider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        if (growContent) {
            VBox.setVgrow(content, Priority.ALWAYS);
        }

        pane.getChildren().addAll(header, headerDivider, content);
        return pane;
    }

    private void initSimulator() {
        try {
            simulator.load(assemblyCode);
            parseSuccess = true;
            instructionLineMap = buildInstructionLineMap();
            terminalContent.setLength(0);
            setTerminalOutput("Parsed successfully. Ready to simulate.");
            highlightCurrentInstruction();
            if (stackView != null) stackView.reset(simulator.getState());
            if (registersView != null) registersView.reset(simulator.getState());
            if (flagsView != null) flagsView.reset(simulator.getState());
            if (memoryView != null) memoryView.reset(simulator.getState(), simulator.getLabelManager());
        } catch (Exception e) {
            parseSuccess = false;
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            setTerminalError(msg);
            setControlsEnabled(false);
            scheduleParseErrorPopup(msg);
        }
    }

    private List<Integer> buildInstructionLineMap() {
        List<Integer> map = new ArrayList<>();
        String[] lines = assemblyCode.split("\\R", -1);
        boolean inText = true;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int commentIdx = line.indexOf('#');
            if (commentIdx >= 0) line = line.substring(0, commentIdx);
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String possibleLabel = line.substring(0, colonIdx).trim();
                if (!possibleLabel.contains(" ") && !possibleLabel.contains("\t")) {
                    line = line.substring(colonIdx + 1).trim();
                }
            }
            if (line.isEmpty()) continue;

            if (line.equals(".text")) { inText = true; continue; }
            if (line.equals(".data") || line.equals(".bss") || line.equals(".rodata")) {
                inText = false; continue;
            }
            if (line.startsWith(".section")) {
                inText = line.contains("text");
                continue;
            }

            if (line.startsWith(".")) continue;

            if (inText) {
                map.add(i);
            }
        }
        return map;
    }

    private void highlightCurrentInstruction() {
        if (!parseSuccess) return;
        int idx = simulator.getCurrentInstructionIndex();
        if (idx >= 0 && idx < instructionLineMap.size()) {
            highlightLine(instructionLineMap.get(idx));
        } else {
            clearHighlight();
        }
    }

    private void clearHighlight() {
        for (HBox row : codeRows) {
            row.setStyle("-fx-background-color: " + BG_EDITOR + ";");
        }
    }

    private void scrollToLine(int lineIndex) {
        if (codeScrollPane == null || codeRows.isEmpty()) return;
        Platform.runLater(() -> {
            double rowHeight = 22.0;
            double totalHeight = codeRows.size() * rowHeight;
            double viewportHeight = codeScrollPane.getViewportBounds().getHeight();
            if (totalHeight <= viewportHeight) return;

            double rowCenterY = lineIndex * rowHeight + rowHeight / 2.0;
            double idealTop = rowCenterY - viewportHeight / 2.0;
            double maxScroll = totalHeight - viewportHeight;
            idealTop = Math.max(0, Math.min(idealTop, maxScroll));
            codeScrollPane.setVvalue(idealTop / maxScroll);
        });
    }

    // ── Execution Logic ───────────────────────────────────────────────────────

    private void handleStep() {
        if (!parseSuccess || simulator.isHalted()) return;

        try {
            Instruction currentInst = simulator.getCurrentInstruction();
            String mnemonic = currentInst != null ? currentInst.getMnemonic().toUpperCase() : "INSTRUCTION";

            StepResult result = simulator.step();
            setStepCount(currentStep + 1);
            setInstructionDescription(result.description(), mnemonic);

            if (result.hasOutput()) {
                appendTerminalOutput(result.output(), TERMINAL_WHITE);
            }

            updateViewPanels();

            if (simulator.isHalted()) {
                handleHalt();
                return;
            }

            if (simulator.isWaitingForInput()) {
                stopAutomatedExecution();
                setControlsEnabled(false);
                activateTerminalInput();
                return;
            }

            highlightCurrentInstruction();

        } catch (Exception e) {
            stopAutomatedExecution();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            appendTerminalError("\nError: " + msg);
            clearHighlight();
            setControlsEnabled(false);
        }
    }

    private void toggleAutoPlay() {
        if (isAutoPlaying) {
            stopAutomatedExecution();
        } else {
            if (simulator.isHalted() || simulator.isWaitingForInput()) return;
            isAutoPlaying = true;
            stepBtn.setDisable(true);
            runToEndBtn.setDisable(true);
            autoPlayBtn.setText("Pause ⏸");
            autoPlayBtn.setStyle(BTN_PRIMARY);

            executionThread = new Thread(() -> {
                while (isAutoPlaying && !simulator.isHalted() && !simulator.isWaitingForInput()) {
                    Platform.runLater(this::handleStep);
                    try {
                        Thread.sleep((long) speedSlider.getValue());
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                Platform.runLater(this::stopAutomatedExecution);
            });
            executionThread.setDaemon(true);
            executionThread.start();
        }
    }

    private void handleRunToEnd() {
        if (!parseSuccess || simulator.isHalted() || simulator.isWaitingForInput()) return;

        isRunningToEnd = true;
        setControlsEnabled(false);
        runToEndBtn.setText("Running...");

        executionThread = new Thread(() -> {
            StringBuilder batchedOutput = new StringBuilder();
            int batchSteps = 0;
            String lastMnemonic = "";
            String lastDesc = "";

            while (isRunningToEnd && !simulator.isHalted() && !simulator.isWaitingForInput()) {
                try {
                    Instruction currentInst = simulator.getCurrentInstruction();
                    lastMnemonic = currentInst != null ? currentInst.getMnemonic().toUpperCase() : "INSTRUCTION";

                    StepResult result = simulator.step();
                    currentStep++;
                    lastDesc = result.description();

                    if (result.hasOutput()) {
                        batchedOutput.append(result.output());
                    }

                    batchSteps++;

                    // Flush to UI every 1000 steps or if halting/waiting to prevent freezing infinite loops
                    if (batchSteps >= 1000 || simulator.isHalted() || simulator.isWaitingForInput()) {
                        boolean finishing = simulator.isHalted() || simulator.isWaitingForInput();
                        batchSteps = 0;
                        int stepSnapshot = currentStep;
                        String descSnapshot = lastDesc;
                        String mnemonicSnapshot = lastMnemonic;

                        if (finishing) {
                            String out = batchedOutput.toString();
                            batchedOutput.setLength(0);
                            boolean haltedSnapshot = simulator.isHalted();
                            boolean waitingSnapshot = simulator.isWaitingForInput();

                            Platform.runLater(() -> {
                                if (!out.isEmpty()) appendTerminalOutput(out, TERMINAL_WHITE);
                                setStepCount(stepSnapshot);
                                setInstructionDescription(descSnapshot, mnemonicSnapshot);
                                updateViewPanels();
                                highlightCurrentInstruction();

                                if (haltedSnapshot) {
                                    handleHalt();
                                } else {
                                    isRunningToEnd = false;
                                    activateTerminalInput();
                                }
                            });
                        } else {
                            Platform.runLater(() -> {
                                setStepCount(stepSnapshot);
                                setInstructionDescription(descSnapshot, mnemonicSnapshot);
                            });
                        }
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    Platform.runLater(() -> {
                        appendTerminalError("\nError: " + msg);
                        clearHighlight();
                    });
                    break;
                }
            }
            Platform.runLater(this::stopAutomatedExecution);
        });
        executionThread.setDaemon(true);
        executionThread.start();
    }

    private void stopAutomatedExecution() {
        isAutoPlaying = false;
        isRunningToEnd = false;
        if (executionThread != null && executionThread.isAlive()) {
            executionThread.interrupt();
        }

        Platform.runLater(() -> {
            autoPlayBtn.setText("Autoplay ▶▶");
            autoPlayBtn.setStyle(BTN_SECONDARY);
            runToEndBtn.setText("Run to End ⏭");
            runToEndBtn.setStyle(BTN_SECONDARY);

            if (!simulator.isHalted() && !simulator.isWaitingForInput()) {
                setControlsEnabled(true);
            }
        });
    }

    private void updateViewPanels() {
        if (stackView != null) stackView.update(simulator.getState());
        if (registersView != null) registersView.update(simulator.getState());
        if (flagsView != null) flagsView.update(simulator.getState());
        if (memoryView != null) memoryView.update(simulator.getState());
    }

    private void handleHalt() {
        stopAutomatedExecution();
        setControlsEnabled(false);
        clearHighlight();
        long exitCode = simulator.getExitCode();
        if (exitCode == 0) {
            appendTerminalOutput("\n[Program exited with code " + exitCode + "]", TERMINAL_GREEN);
        } else {
            appendTerminalOutput("\n[Program exited with code " + exitCode + "]", RED_TEXT);
        }
    }

    private void handleReset() {
        stopAutomatedExecution();
        if (!parseSuccess) return;
        try {
            simulator.reset();
            terminalContent.setLength(0);
            setTerminalOutput("Reset. Ready to simulate.");
            setInstructionDescription(null, null);
            setStepCount(0);
            setControlsEnabled(true);

            if (terminalInputActive) deactivateTerminalInput();

            instructionLineMap = buildInstructionLineMap();
            highlightCurrentInstruction();

            if (stackView != null) stackView.reset(simulator.getState());
            if (registersView != null) registersView.reset(simulator.getState());
            if (flagsView != null) flagsView.reset(simulator.getState());
            if (memoryView != null) memoryView.reset(simulator.getState(), simulator.getLabelManager());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            setTerminalError(msg);
        }
    }

    private void handleTerminalInput() {
        String input = terminalInputField.getText();
        addTerminalRow(input, TERMINAL_WHITE, true);
        try {
            simulator.provideInput(input);
        } catch (Exception e) {
            deactivateTerminalInput();
            appendTerminalError("\n[Input error] " + e.getMessage());
            return;
        }
        deactivateTerminalInput();
        setControlsEnabled(true);
        // Process the instruction that was waiting for input
        handleStep();
    }

    private void setControlsEnabled(boolean enabled) {
        if (stepBtn != null) stepBtn.setDisable(!enabled);
        if (autoPlayBtn != null) autoPlayBtn.setDisable(!enabled);
        if (runToEndBtn != null) runToEndBtn.setDisable(!enabled);
    }

    private void scheduleParseErrorPopup(String errorMessage) {
        sceneProperty().addListener((obsSc, oldSc, newSc) -> {
            if (newSc == null) return;
            Runnable showPopup = () -> Platform.runLater(() ->
                    ErrorDialog.showParseError(newSc.getWindow()));
            if (newSc.getWindow() != null) {
                showPopup.run();
            } else {
                newSc.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin != null) showPopup.run();
                });
            }
        });
    }

    private HBox buildPaneHeader(String title) {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 12, 0, 14));
        header.setPrefHeight(34);
        header.setMinHeight(34);
        header.setStyle("-fx-background-color: " + BG_RAISED + ";" +
                "-fx-background-radius: 6 6 0 0;");

        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-letter-spacing: 1;"
        );

        header.getChildren().add(titleLabel);
        return header;
    }

    private Node buildRegistersContent() {
        registersView = new RegistersView(simulator.getState());
        VBox.setVgrow(registersView, Priority.ALWAYS);
        return registersView;
    }

    private Node buildFlagsContent() {
        flagsView = new FlagsView(simulator.getState());
        VBox.setVgrow(flagsView, Priority.ALWAYS);
        return flagsView;
    }

    private Node buildStackContent() {
        stackView = new StackView(simulator.getState());
        VBox.setVgrow(stackView, Priority.ALWAYS);
        return stackView;
    }

    private Node buildMemoryContent() {
        memoryView = new MemoryView(simulator.getState(), simulator.getLabelManager());
        VBox.setVgrow(memoryView, Priority.ALWAYS);
        return memoryView;
    }

    // ── Terminal (Bottom) ─────────────────────────────────────────────────────

    private VBox buildTerminal() {
        terminalPane = new VBox(0);
        terminalPane.setPrefHeight(175);
        terminalPane.setMinHeight(145);
        terminalPane.setMaxHeight(Double.MAX_VALUE);
        terminalPane.setStyle(
                "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;"
        );

        HBox terminalHeader = buildTerminalHeader();

        ScrollPane outputScroll = buildTerminalOutput();
        VBox.setVgrow(outputScroll, Priority.ALWAYS);

        HBox inputRow = buildTerminalInputRow();

        terminalPane.getChildren().addAll(terminalHeader, outputScroll, inputRow);
        return terminalPane;
    }

    private HBox buildTerminalHeader() {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(28);
        header.setPadding(new Insets(0, 12, 0, 14));
        header.setStyle(
                "-fx-background-color: #1A1C1E;" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        Label title = new Label("Terminal");
        title.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Input enabled on scanf");
        hint.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10;" +
                        "-fx-text-fill: #444849;"
        );

        header.getChildren().addAll(title, spacer, hint);
        return header;
    }

    private ScrollPane buildTerminalOutput() {
        terminalFlow = new TextFlow();
        terminalFlow.setStyle("-fx-background-color: " + TERMINAL_BG + ";");
        terminalFlow.setPadding(new Insets(2, 14, 2, 14));
        terminalFlow.setLineSpacing(0);

        terminalScroll = new ScrollPane(terminalFlow);
        terminalScroll.setFitToWidth(false);
        terminalScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        terminalScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        terminalScroll.setStyle(
                "-fx-background: " + TERMINAL_BG + ";" +
                        "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: transparent;"
        );
        terminalScroll.getStylesheets().add(DARK_SCROLL_CSS);
        VBox.setVgrow(terminalScroll, Priority.ALWAYS);

        terminalScroll.viewportBoundsProperty().addListener((obs, oldB, bounds) -> {
            if (bounds != null) terminalFlow.setMinWidth(bounds.getWidth());
        });
        terminalFlow.heightProperty().addListener((obs, oldVal, newVal) ->
                terminalScroll.setVvalue(1.0)
        );

        // Seed with initial message
        addTerminalRow("No program running.", TEXT_MUTED, false);

        return terminalScroll;
    }

    private void addTerminalRow(String text, String color, boolean withArrow) {
        String line = withArrow ? "❯ " + text + "\n" : text + "\n";
        if (!terminalFlow.getChildren().isEmpty()) line = "\n" + line;
        Text node = new Text(line);
        node.setFont(Font.font("JetBrains Mono", 12));
        node.setFill(Color.web(color));
        terminalFlow.getChildren().add(node);
        scrollTerminalToBottom();
    }

    private void scrollTerminalToBottom() {
        Platform.runLater(() -> {
            if (terminalScroll != null) terminalScroll.setVvalue(1.0);
        });
    }

    private HBox buildTerminalInputRow() {
        Label prompt = new Label("❯");
        prompt.setPadding(new Insets(0, 8, 0, 14));
        prompt.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        terminalInputField = new TextField();
        terminalInputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && terminalInputActive) {
                handleTerminalInput();
            }
        });
        terminalInputField.setEditable(false);
        terminalInputField.setPromptText("No input requested yet.");
        terminalInputField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-prompt-text-fill: #333537;"
        );
        HBox.setHgrow(terminalInputField, Priority.ALWAYS);

        HBox inputRow = new HBox(0, prompt, terminalInputField);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPrefHeight(30);
        inputRow.setMinHeight(30);
        inputRow.setStyle(
                "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;"
        );
        return inputRow;
    }

    public void setTerminalOutput(String text) {
        if (terminalFlow == null) return;
        terminalFlow.getChildren().clear();
        addTerminalRow(text, TERMINAL_GREEN, false);
    }

    public void setTerminalError(String message) {
        if (terminalFlow == null) return;
        terminalFlow.getChildren().clear();
        addTerminalRow(message, RED_TEXT, false);
    }

    private void appendTerminalOutput(String text, String color) {
        if (text == null || text.isEmpty()) return;
        Text node = new Text(text);
        node.setFont(TERMINAL_FONT);
        node.setFill(Color.web(color));
        terminalFlow.getChildren().add(node);
        scrollTerminalToBottom();
    }

    private void appendTerminalError(String text) {
        if (text == null || text.isEmpty()) return;
        Text node = new Text(text);
        node.setFont(TERMINAL_FONT);
        node.setFill(Color.web(RED_TEXT));
        terminalFlow.getChildren().add(node);
        scrollTerminalToBottom();
    }

    public void activateTerminalInput() {
        terminalInputActive = true;
        terminalInputField.setEditable(true);
        terminalInputField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TERMINAL_GREEN + ";" +
                        "-fx-prompt-text-fill: #4A5052;"
        );
        terminalPane.setStyle(
                "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(78,201,78,0.18), 20, 0.0, 0, -4);"
        );
        terminalInputField.requestFocus();
    }

    public void deactivateTerminalInput() {
        terminalInputActive = false;
        terminalInputField.setEditable(false);
        terminalInputField.clear();
        terminalInputField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-prompt-text-fill: #333537;"
        );
        terminalPane.setStyle(
                "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;"
        );
    }

    public void highlightLine(int lineIndex) {
        for (int i = 0; i < codeRows.size(); i++) {
            codeRows.get(i).setStyle(
                    "-fx-background-color: " + (i == lineIndex ? ACTIVE_LINE_BG : BG_EDITOR) + ";"
            );
        }
        scrollToLine(lineIndex);
    }

    public void setInstructionDescription(String description, String mnemonic) {
        if (description == null || description.isBlank()) {
            if (instructionTagLabel != null) {
                instructionTagLabel.setText("AWAITING EXECUTION");
                instructionTagLabel.setStyle(
                        "-fx-background-color: #2A2A2A;" +
                                "-fx-background-radius: 3;" +
                                "-fx-border-color: " + BORDER_SOFT + ";" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 3;" +
                                "-fx-font-family: " + SANS + ";" +
                                "-fx-font-size: 9;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: " + TEXT_MUTED + ";" +
                                "-fx-letter-spacing: 0.6;"
                );
            }
            instructionDescLabel.setText("A simple explanation of each executed instruction" +
                    " will be visible here");
            instructionDescLabel.setStyle(
                    "-fx-font-family: " + SANS + ";" +
                            "-fx-font-size: 12;" +
                            "-fx-text-fill: #555759;" +
                            "-fx-line-spacing: 2;"
            );
        } else {
            if (instructionTagLabel != null) {
                instructionTagLabel.setText(mnemonic);
                instructionTagLabel.setStyle(
                        "-fx-background-color: #2E2508;" +
                                "-fx-background-radius: 3;" +
                                "-fx-border-color: #5A4010;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 3;" +
                                "-fx-font-family: " + SANS + ";" +
                                "-fx-font-size: 9;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: " + AMBER + ";" +
                                "-fx-letter-spacing: 0.6;"
                );
            }
            instructionDescLabel.setText(description);
            instructionDescLabel.setStyle(
                    "-fx-font-family: " + SANS + ";" +
                            "-fx-font-size: 12;" +
                            "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                            "-fx-line-spacing: 2;"
            );
        }
    }

    public void setStepCount(int step) {
        currentStep = step;
        stepCounterLabel.setText("step  " + step);
    }
}