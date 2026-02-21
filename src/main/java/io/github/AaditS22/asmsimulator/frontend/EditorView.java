package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.frontend.util.AsmHighlighter;
import io.github.AaditS22.asmsimulator.frontend.util.ConfirmDialog;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.IntFunction;

// DISCLAIMER: This class was largely written with the help of LLMs
public class EditorView extends VBox {

    private static final String BG_BASE      = "#2B2B2B";
    private static final String BG_EDITOR    = "#1E1F22";
    private static final String BG_GUTTER    = "#252527";
    private static final String BG_PANEL     = "#313335";
    private static final String BG_RAISED    = "#3C3F41";
    private static final String BG_HOVER     = "#4C5052";
    private static final String BORDER_SOFT  = "#424547";
    private static final String AMBER        = "#E8A845";
    private static final String TEXT_PRIMARY = "#BBBBBB";
    private static final String TEXT_BRIGHT  = "#E8E8E8";
    private static final String TEXT_MUTED   = "#777777";
    private static final String GUTTER_TEXT  = "#555555";

    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    private static final String BTN_DEFAULT =
            "-fx-background-color: " + BG_RAISED + ";" +
                    "-fx-text-fill: " + AMBER + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 13;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 11 32 11 32;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + AMBER + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_HOVER =
            "-fx-background-color: " + AMBER + ";" +
                    "-fx-text-fill: #1E1E1E;" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 13;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 11 32 11 32;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + AMBER + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_UPLOAD =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 8 20 8 20;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + BORDER_SOFT + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String BTN_UPLOAD_HOVER =
            "-fx-background-color: " + BG_HOVER + ";" +
                    "-fx-text-fill: " + TEXT_BRIGHT + ";" +
                    "-fx-font-family: " + SANS + ";" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 8 20 8 20;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: " + BORDER_SOFT + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";

    private static final String PLACEHOLDER =
            """
                    # Example code
                    .text
                        .global main
                    main:
                        movq $0, %rdi
                        call exit
                    """;

    private final InlineCssTextArea codeArea;
    private final Label statusLabel;
    private final PauseTransition highlightDebounce;

    public EditorView(Runnable onBack) {
        setStyle("-fx-background-color: " + BG_BASE + ";");
        VBox.setVgrow(this, Priority.ALWAYS);

        statusLabel = buildStatusLabel();
        codeArea = buildCodeArea();
        highlightDebounce = new PauseTransition(Duration.millis(120));
        highlightDebounce.setOnFinished(e -> applyHighlighting());

        getChildren().addAll(
                buildTopBar(onBack),
                buildMainContent()
        );
    }

    private HBox buildTopBar(Runnable onBack) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        VBox.setVgrow(bar, Priority.NEVER);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;"
        );
        Label project = new Label("x86-64 AT&T Assembly Simulator");
        project.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label backBtn = new Label("← Home");
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
                        "-fx-font-size: 13;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-cursor: hand;";
        String closeHover =
                "-fx-background-color: #C0392B;" +
                        "-fx-background-radius: 4;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 13;" +
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
        bar.getChildren().addAll(project, spacer, backBtn, closeBtn);
        return bar;
    }

    private HBox buildMainContent() {
        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setStyle("-fx-background-color: " + BG_BASE + ";");
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox leftPanel = buildLeftPanel();
        VBox editorPanel = buildEditorPanel();

        HBox.setHgrow(editorPanel, Priority.ALWAYS);

        content.getChildren().addAll(leftPanel, editorPanel);

        return content;
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(230);
        panel.setMinWidth(210);
        panel.setMaxWidth(250);
        panel.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0.1, 0, 6);"
        );

        VBox referenceContent = buildReferenceContent();
        VBox.setVgrow(referenceContent, Priority.ALWAYS);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        VBox buttonSection = buildSimulatorButtonSection();

        panel.getChildren().addAll(referenceContent, divider, buttonSection);
        return panel;
    }

    private VBox buildReferenceContent() {
        VBox container = new VBox(0);
        VBox.setVgrow(container, Priority.ALWAYS);

        Label header = new Label("QUICK GUIDE");
        header.setPadding(new Insets(16, 16, 12, 16));
        header.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-letter-spacing: 1.5;"
        );

        Region headerDivider = new Region();
        headerDivider.setPrefHeight(1);
        headerDivider.setMaxWidth(Double.MAX_VALUE);
        headerDivider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(2);
        content.setPadding(new Insets(8, 0, 8, 0));
        content.setStyle("-fx-background-color: transparent;");

        content.getChildren().addAll(
                refSection("Data Transfer"),
                refEntry("movq", "src, dst", "Copy src to dst"),
                refEntry("leaq", "addr, dst", "Load address"),
                refEntry("pushq", "src", "Push to stack"),
                refEntry("popq", "dst", "Pop from stack"),

                refSpacer(),
                refSection("Arithmetic"),
                refEntry("addq", "src, dst", "dst += src"),
                refEntry("subq", "src, dst", "dst -= src"),
                refEntry("imulq", "src, dst", "dst *= src"),
                refEntry("idivq", "src", "rdx:rax / src"),
                refEntry("incq", "dst", "dst++"),
                refEntry("decq", "dst", "dst--"),
                refEntry("negq", "dst", "dst = -dst"),

                refSpacer(),
                refSection("Logical"),
                refEntry("andq", "src, dst", "Bitwise AND"),
                refEntry("orq", "src, dst", "Bitwise OR"),
                refEntry("xorq", "src, dst", "Bitwise XOR"),
                refEntry("notq", "dst", "Bitwise NOT"),
                refEntry("shlq", "amt, dst", "Shift left"),
                refEntry("shrq", "amt, dst", "Shift right"),

                refSpacer(),
                refSection("Control Flow"),
                refEntry("cmpq", "a, b", "Set flags (b-a)"),
                refEntry("jmp", "label", "Unconditional jump"),
                refEntry("je / jne", "label", "Jump if equal/not"),
                refEntry("jg / jl", "label", "Jump if greater/less"),
                refEntry("call", "label", "Call subroutine"),
                refEntry("ret", "", "Return"),

                refSpacer(),
                refSection("General Registers"),
                refEntry("%rax", "", "caller-saved"),
                refEntry("%rdx", "", "caller-saved"),
                refEntry("%rcx", "", "caller-saved"),
                refEntry("%r8-%r11", "", "caller-saved"),
                refEntry("%rbx", "", "callee-saved"),
                refEntry("%r12-%r15", "", "callee-saved"),

                refSpacer(),
                refSection("Special Registers"),
                refEntry("%rdi / %rsi", "", "Arg 1 / Arg 2"),
                refEntry("%rsp", "", "Stack pointer"),
                refEntry("%rbp", "", "Base pointer"),
                refEntry("%rip", "", "Instruction pointer")
        );

        scroll.setContent(content);

        container.getChildren().addAll(header, headerDivider, scroll);
        return container;
    }

    private Label refSection(String title) {
        Label lbl = new Label(title.toUpperCase());
        lbl.setPadding(new Insets(10, 16, 4, 16));
        lbl.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 9.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";" +
                        "-fx-letter-spacing: 1;"
        );
        return lbl;
    }

    private Region refSpacer() {
        Region r = new Region();
        r.setPrefHeight(4);
        return r;
    }

    private VBox refEntry(String mnemonic, String operands, String description) {
        Label mnemonicLbl = new Label(mnemonic);
        mnemonicLbl.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: #E8A845;" +
                        "-fx-font-weight: bold;"
        );

        Label operandsLbl = new Label(operands.isEmpty() ? "" : " " + operands);
        operandsLbl.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: #56A6E8;"
        );

        HBox topRow = new HBox(0, mnemonicLbl, operandsLbl);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label descLbl = new Label(description);
        descLbl.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        VBox entry = new VBox(1, topRow, descLbl);
        entry.setPadding(new Insets(3, 16, 3, 16));
        entry.setStyle("-fx-background-color: transparent;");

        String hoverStyle = "-fx-background-color: " + BG_RAISED + ";";
        entry.setOnMouseEntered(e -> entry.setStyle(hoverStyle));
        entry.setOnMouseExited(e -> entry.setStyle("-fx-background-color: transparent;"));

        return entry;
    }

    private VBox buildSimulatorButtonSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(16, 16, 18, 16));
        section.setAlignment(Pos.CENTER);

        Label hint = new Label("Load code into the simulator");
        hint.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-text-alignment: center;" +
                        "-fx-line-spacing: 2;"
        );
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setAlignment(Pos.CENTER);

        Button simBtn = new Button("Simulate");
        simBtn.setMaxWidth(Double.MAX_VALUE);
        simBtn.setStyle(BTN_DEFAULT);
        simBtn.setOnMouseEntered(e -> { if (!simBtn.isDisabled()) simBtn.setStyle(BTN_HOVER); });
        simBtn.setOnMouseExited(e -> simBtn.setStyle(BTN_DEFAULT));
        simBtn.setDisable(true);

        section.getChildren().addAll(hint, simBtn);
        return section;
    }

    private VBox buildEditorPanel() {
        VBox panel = new VBox(0);
        panel.setStyle(
                "-fx-background-color: " + BG_EDITOR + ";" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 24, 0.1, 0, 8);"
        );
        VBox.setVgrow(panel, Priority.ALWAYS);

        HBox editorTopBar = buildEditorTopBar();
        StackPane editorArea = buildEditorArea();
        HBox editorStatusBar = buildEditorStatusBar();

        VBox.setVgrow(editorArea, Priority.ALWAYS);
        panel.getChildren().addAll(editorTopBar, editorArea, editorStatusBar);
        return panel;
    }

    private HBox buildEditorTopBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 14, 0, 14));
        bar.setPrefHeight(38);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 6 6 0 0;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadBtn = new Button("Upload .s/.asm File");
        uploadBtn.setStyle(BTN_UPLOAD);
        uploadBtn.setOnMouseEntered(e -> uploadBtn.setStyle(BTN_UPLOAD_HOVER));
        uploadBtn.setOnMouseExited(e -> uploadBtn.setStyle(BTN_UPLOAD));
        uploadBtn.setOnAction(e -> handleFileUpload());

        bar.getChildren().addAll(spacer, uploadBtn);
        return bar;
    }

    private StackPane buildEditorArea() {
        StackPane wrapper = new StackPane();
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VirtualizedScrollPane<InlineCssTextArea> scroll = new VirtualizedScrollPane<>(codeArea);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        StackPane.setAlignment(scroll, Pos.TOP_LEFT);

        wrapper.getChildren().add(scroll);
        return wrapper;
    }

    private InlineCssTextArea buildCodeArea() {
        InlineCssTextArea area = new InlineCssTextArea();
        area.setWrapText(false);
        area.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 13.5;" +
                        "-fx-background-color: " + BG_EDITOR + ";" +
                        "-fx-padding: 0;"
        );

        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(area);
        area.setParagraphGraphicFactory(line -> {
            Node node = lineNumberFactory.apply(line);
            node.setStyle(
                    "-fx-font-family: " + MONO + ";" +
                            "-fx-font-size: 12;" +
                            "-fx-text-fill: " + GUTTER_TEXT + ";" +
                            "-fx-padding: 0 8 0 8;" +
                            "-fx-background-color: " + BG_GUTTER + ";" +
                            "-fx-pref-width: 56;" +
                            "-fx-min-width: 56;" +
                            "-fx-alignment: CENTER_RIGHT;"
            );
            return node;
        });

        area.replaceText(PLACEHOLDER);
        applyHighlightingTo(area, PLACEHOLDER);

        area.textProperty().addListener((obs, oldText, newText) -> {
            updateStatus(newText);
            highlightDebounce.playFromStart();
        });

        updateStatus(PLACEHOLDER);
        return area;
    }

    private Label buildStatusLabel() {
        Label lbl = new Label();
        lbl.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );
        return lbl;
    }

    private HBox buildEditorStatusBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 14, 4, 14));
        bar.setPrefHeight(26);
        bar.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 0 0 6 6;"
        );

        Label langLabel = new Label("x86-64 AT&T");
        langLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(langLabel, spacer, statusLabel);
        return bar;
    }

    private void applyHighlighting() {
        applyHighlightingTo(codeArea, codeArea.getText());
    }

    private void applyHighlightingTo(InlineCssTextArea area, String text) {
        if (text.isEmpty()) return;
        try {
            area.setStyleSpans(0, AsmHighlighter.computeHighlighting(text));
        } catch (Exception ignored) {
        }
    }

    private void updateStatus(String text) {
        int lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
        int chars = text.length();
        statusLabel.setText(lines + "L  " + chars + "C");
    }

    private void handleFileUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Assembly File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Assembly Files", "*.s", "*.asm", "*.S"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        Stage stage = (Stage) getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                codeArea.replaceText(content);
            } catch (IOException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * Gets the current user code
     * @return the current user code
     */
    public String getCode() {
        return codeArea.getText();
    }
}