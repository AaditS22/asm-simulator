package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.frontend.util.AsmHighlighter;
import io.github.AaditS22.asmsimulator.frontend.util.ConfirmDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.function.IntFunction;

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

    private static final String HIGHLIGHT_BG     = "#2E2910";
    private static final String HIGHLIGHT_BORDER = "#7A5820";

    private static final String TERMINAL_BG    = "#141618";
    private static final String TERMINAL_GREEN = "#4EC94E";

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

    private InlineCssTextArea codeArea;
    private Label stepCounterLabel;
    private Label terminalOutputLabel;
    private TextField terminalInputField;
    private VBox terminalPane;
    private Label instructionDescLabel;
    private boolean terminalInputActive = false;

    private final Runnable onBack;
    private final String assemblyCode;
    private int currentStep = 0;

    public SimulatorView(Runnable onBack, String assemblyCode) {
        this.onBack = onBack;
        this.assemblyCode = assemblyCode == null ? "" : assemblyCode;
        buildUI();
    }

    private void buildUI() {
        setStyle("-fx-background-color: " + BG_BASE + ";");
        VBox.setVgrow(this, Priority.ALWAYS);

        HBox titleBar    = buildTitleBar();
        HBox mainContent = buildMainContent();
        VBox terminal    = buildTerminal();

        VBox.setVgrow(mainContent, Priority.ALWAYS);
        getChildren().addAll(titleBar, mainContent, terminal);
    }

    // ── Title Bar ──────────────────────────────────────────────────────────────

    private HBox buildTitleBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(42);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );

        HBox brandBox = buildBrand();
        HBox.setMargin(brandBox, new Insets(0, 0, 0, 16));

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        HBox controls = buildControlButtons();

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox windowBtns = buildWindowButtons();
        HBox.setMargin(windowBtns, new Insets(0, 12, 0, 0));

        bar.getChildren().addAll(brandBox, leftSpacer, controls, rightSpacer, windowBtns);
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

    private HBox buildControlButtons() {
        Button restartBtn = new Button("⟳  Restart");
        restartBtn.setStyle(BTN_SECONDARY);
        restartBtn.setOnMouseEntered(e -> restartBtn.setStyle(BTN_SECONDARY_HOVER));
        restartBtn.setOnMouseExited(e -> restartBtn.setStyle(BTN_SECONDARY));

        Button stepBtn = new Button("Step  ▶");
        stepBtn.setStyle(BTN_PRIMARY);
        stepBtn.setOnMouseEntered(e -> stepBtn.setStyle(BTN_PRIMARY_HOVER));
        stepBtn.setOnMouseExited(e -> stepBtn.setStyle(BTN_PRIMARY));

        stepCounterLabel = new Label("step  0");
        stepCounterLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 0 4 0 4;"
        );

        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setPrefHeight(18);
        divider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");
        HBox.setMargin(divider, new Insets(0, 4, 0, 4));

        HBox controls = new HBox(8, restartBtn, stepBtn, divider, stepCounterLabel);
        controls.setAlignment(Pos.CENTER);
        return controls;
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
        backBtn.setOnMouseClicked(e -> onBack.run());

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

        HBox windowBtns = new HBox(6, backBtn, closeBtn);
        windowBtns.setAlignment(Pos.CENTER_RIGHT);
        return windowBtns;
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

        codeArea = buildCodeArea();
        VirtualizedScrollPane<InlineCssTextArea> scroll = new VirtualizedScrollPane<>(codeArea);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        StackPane editorWrapper = new StackPane(scroll);
        editorWrapper.setStyle("-fx-background-color: " + BG_EDITOR + ";");
        VBox.setVgrow(editorWrapper, Priority.ALWAYS);

        pane.getChildren().addAll(header, editorWrapper, buildInstructionDescPane());
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

    private InlineCssTextArea buildCodeArea() {
        InlineCssTextArea area = new InlineCssTextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-background-color: " + BG_EDITOR + ";" +
                        "-fx-padding: 0;"
        );

        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(area);
        area.setParagraphGraphicFactory(line -> {
            Node node = lineNumberFactory.apply(line);
            node.setStyle(
                    "-fx-font-family: " + MONO + ";" +
                            "-fx-font-size: 11.5;" +
                            "-fx-text-fill: " + GUTTER_TEXT + ";" +
                            "-fx-padding: 0 8 0 8;" +
                            "-fx-background-color: " + BG_GUTTER + ";" +
                            "-fx-pref-width: 48;" +
                            "-fx-min-width: 48;" +
                            "-fx-alignment: CENTER_RIGHT;"
            );
            return node;
        });

        if (!assemblyCode.isEmpty()) {
            area.replaceText(assemblyCode);
            try {
                area.setStyleSpans(0, AsmHighlighter.computeHighlighting(assemblyCode));
            } catch (Exception ignored) {
            }
        }

        return area;
    }

    // ── Instruction Description Pane ──────────────────────────────────────────

    private VBox buildInstructionDescPane() {
        VBox pane = new VBox(0);
        pane.setMinHeight(80);
        pane.setPrefHeight(80);
        pane.setMaxHeight(80);
        pane.setStyle(
                "-fx-background-color: #222426;" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;"
        );

        HBox labelRow = new HBox(0);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        labelRow.setPadding(new Insets(0, 14, 0, 14));
        labelRow.setPrefHeight(22);

        Label tag = new Label("INSTRUCTION EXPLANATION");
        tag.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #4A5052;" +
                        "-fx-letter-spacing: 0.8;"
        );
        labelRow.getChildren().add(tag);

        instructionDescLabel = new Label("No instruction executed yet.");
        instructionDescLabel.setWrapText(true);
        instructionDescLabel.setMaxWidth(Double.MAX_VALUE);
        instructionDescLabel.setPadding(new Insets(0, 14, 8, 14));
        instructionDescLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-line-spacing: 1;"
        );

        pane.getChildren().addAll(labelRow, instructionDescLabel);
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
        // Stack spans the full height on the left
        VBox stackPane = buildPane("Stack", buildStackContent(), true);
        stackPane.setMinWidth(260);
        stackPane.setPrefWidth(280);
        stackPane.setMaxWidth(320);

        // Right column: Registers+Flags on top (shorter), Memory on bottom (larger)
        VBox rightColumn = buildRightColumn();
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox row = new HBox(12, stackPane, rightColumn);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(row, Priority.ALWAYS);
        return row;
    }

    private VBox buildRightColumn() {
        HBox topRow = buildTopPaneRow();
        topRow.setMinHeight(170);
        topRow.setPrefHeight(180);
        topRow.setMaxHeight(210);

        Region rowGap = new Region();
        rowGap.setPrefHeight(12);
        rowGap.setMinHeight(12);
        rowGap.setMaxHeight(12);

        VBox memoryPane = buildPane("Memory", buildMemoryContent(), true);
        VBox.setVgrow(memoryPane, Priority.ALWAYS);

        VBox col = new VBox(0, topRow, rowGap, memoryPane);
        VBox.setVgrow(col, Priority.ALWAYS);
        return col;
    }

    private HBox buildTopPaneRow() {
        VBox registersPane = buildPane("Registers", buildRegistersContent(), true);
        VBox flagsPane     = buildPane("Flags",     buildFlagsContent(),     false);

        flagsPane.setMinWidth(158);
        flagsPane.setPrefWidth(162);
        flagsPane.setMaxWidth(162);

        HBox.setHgrow(registersPane, Priority.ALWAYS);

        HBox row = new HBox(12, registersPane, flagsPane);
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

    private HBox buildPaneHeader(String title) {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 12, 0, 14));
        header.setPrefHeight(34);
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

    // ── Pane Placeholder Content ───────────────────────────────────────────────

    private Node buildRegistersContent() {
        Label placeholder = new Label("Registers will appear here during simulation.");
        placeholder.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 12 14 12 14;"
        );
        placeholder.setWrapText(true);

        StackPane wrapper = new StackPane(placeholder);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private Node buildFlagsContent() {
        VBox content = new VBox(8);
        content.setPadding(new Insets(12, 14, 12, 14));
        content.setAlignment(Pos.TOP_LEFT);

        String[] flagNames = {"ZF", "CF", "SF", "OF"};
        String[] flagDescs = {"Zero", "Carry", "Sign", "Overflow"};
        for (int i = 0; i < flagNames.length; i++) {
            content.getChildren().add(buildFlagRow(flagNames[i], flagDescs[i]));
        }

        return content;
    }

    private HBox buildFlagRow(String flagName, String flagDesc) {
        Label nameLabel = new Label(flagName);
        nameLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + BLUE + ";" +
                        "-fx-pref-width: 28;"
        );

        Label descLabel = new Label(flagDesc);
        descLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );
        HBox.setHgrow(descLabel, Priority.ALWAYS);

        Label valueChip = new Label("—");
        valueChip.setPadding(new Insets(1, 7, 1, 7));
        valueChip.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-background-radius: 3;" +
                        "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        HBox row = new HBox(8, nameLabel, descLabel, valueChip);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node buildStackContent() {
        Label placeholder = new Label("Stack frames will\nappear here.");
        placeholder.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 12 14 12 14;"
        );

        StackPane wrapper = new StackPane(placeholder);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private Node buildMemoryContent() {
        Label placeholder = new Label("Memory addresses will appear here during simulation.");
        placeholder.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 12 14 12 14;"
        );
        placeholder.setWrapText(true);

        StackPane wrapper = new StackPane(placeholder);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    // ── Terminal (Bottom) ─────────────────────────────────────────────────────

    private VBox buildTerminal() {
        terminalPane = new VBox(0);
        terminalPane.setPrefHeight(175);
        terminalPane.setMinHeight(145);
        terminalPane.setMaxHeight(240);
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

        Label dot1 = terminalDot("#FF5F56");
        Label dot2 = terminalDot("#FFBD2E");
        Label dot3 = terminalDot("#27C93F");
        HBox dots = new HBox(5, dot1, dot2, dot3);
        dots.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(dots, new Insets(0, 10, 0, 0));

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

        header.getChildren().addAll(dots, title, spacer, hint);
        return header;
    }

    private Label terminalDot(String color) {
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 8;");
        return dot;
    }

    private ScrollPane buildTerminalOutput() {
        terminalOutputLabel = new Label("No program running.");
        terminalOutputLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-padding: 6 14 6 14;"
        );
        terminalOutputLabel.setWrapText(true);
        terminalOutputLabel.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(terminalOutputLabel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: " + TERMINAL_BG + ";" +
                        "-fx-background-color: " + TERMINAL_BG + ";" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
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
        terminalInputField.setEditable(false);
        terminalInputField.setPromptText("Waiting for input...");
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

    // ── Public API (stubs wired up later) ─────────────────────────────────────

    public void setTerminalError(String message) {
        terminalOutputLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + RED_TEXT + ";" +
                        "-fx-padding: 6 14 6 14;"
        );
        terminalOutputLabel.setText("error: " + message);
    }

    public void setTerminalOutput(String text) {
        terminalOutputLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TERMINAL_GREEN + ";" +
                        "-fx-padding: 6 14 6 14;"
        );
        terminalOutputLabel.setText(text);
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
        if (codeArea == null) { return; }
        int lines = codeArea.getParagraphs().size();
        for (int i = 0; i < lines; i++) {
            int len = codeArea.getParagraphLength(i);
            if (i == lineIndex) {
                codeArea.setStyle(i, 0, len,
                        "-fx-background-color: " + HIGHLIGHT_BG + ";"
                );
            } else {
                codeArea.setStyle(i, 0, len, "-fx-background-color: transparent;");
            }
        }
    }

    public void setInstructionDescription(String description) {
        if (description == null || description.isBlank()) {
            instructionDescLabel.setText("No instruction executed yet.");
            instructionDescLabel.setStyle(
                    "-fx-font-family: " + SANS + ";" +
                            "-fx-font-size: 11.5;" +
                            "-fx-text-fill: " + TEXT_MUTED + ";" +
                            "-fx-line-spacing: 1;"
            );
        } else {
            instructionDescLabel.setText(description);
            instructionDescLabel.setStyle(
                    "-fx-font-family: " + SANS + ";" +
                            "-fx-font-size: 11.5;" +
                            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                            "-fx-line-spacing: 1;"
            );
        }
    }

    public void setStepCount(int step) {
        currentStep = step;
        stepCounterLabel.setText("step  " + step);
    }
}