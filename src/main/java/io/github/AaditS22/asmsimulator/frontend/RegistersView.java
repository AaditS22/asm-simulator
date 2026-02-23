package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RegistersView extends VBox {

    private record SubRegInfo(String name32, String name16, String name8) {}

    private static final LinkedHashMap<String, SubRegInfo> REG_INFO = new LinkedHashMap<>();
    static {
        REG_INFO.put("rax", new SubRegInfo("eax", "ax", "al"));
        REG_INFO.put("rbx", new SubRegInfo("ebx", "bx", "bl"));
        REG_INFO.put("rcx", new SubRegInfo("ecx", "cx", "cl"));
        REG_INFO.put("rdx", new SubRegInfo("edx", "dx", "dl"));
        REG_INFO.put("rsi", new SubRegInfo("esi", "si", "sil"));
        REG_INFO.put("rdi", new SubRegInfo("edi", "di", "dil"));
        REG_INFO.put("r8",  new SubRegInfo("r8d",  "r8w",  "r8b"));
        REG_INFO.put("r9",  new SubRegInfo("r9d",  "r9w",  "r9b"));
        REG_INFO.put("r10", new SubRegInfo("r10d", "r10w", "r10b"));
        REG_INFO.put("r11", new SubRegInfo("r11d", "r11w", "r11b"));
        REG_INFO.put("r12", new SubRegInfo("r12d", "r12w", "r12b"));
        REG_INFO.put("r13", new SubRegInfo("r13d", "r13w", "r13b"));
        REG_INFO.put("r14", new SubRegInfo("r14d", "r14w", "r14b"));
        REG_INFO.put("r15", new SubRegInfo("r15d", "r15w", "r15b"));
    }

    private static final String BG_RAISED   = "#3C3F41";
    private static final String BG_HOVER    = "#4C5052";
    private static final String BORDER_SOFT = "#424547";
    private static final String AMBER       = "#E8A845";
    private static final String BLUE        = "#56A6E8";
    private static final String TEXT_PRIMARY = "#BBBBBB";
    private static final String TEXT_BRIGHT = "#E8E8E8";
    private static final String TEXT_MUTED  = "#777777";
    private static final String CHANGED_BG  = "#2B1E00";
    private static final String CHANGED_BDR = "#5A4010";
    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    private static final String BIT_DEFAULT   = "#2A2C2E";
    private static final String BIT_EAX       = "#1A2A3A";
    private static final String BIT_AX        = "#1E3A50";
    private static final String BIT_AL        = "#254A65";
    private static final String BIT_BORDER    = "#3A3C3E";
    private static final String BIT_ONE_TEXT  = TEXT_BRIGHT;
    private static final String BIT_ZERO_TEXT = "#555555";

    private static final String HIGHLIGHT_GLOW   = "#3A5A80";
    private static final String HIGHLIGHT_BORDER = "#5A8ABF";

    private static final String DARK_SCROLL_CSS;
    static {
        String css =
                ".scroll-bar:horizontal,.scroll-bar:vertical{" +
                        "-fx-background-color:#1E1F22;-fx-background-radius:0;}" +
                        ".scroll-bar:horizontal .thumb,.scroll-bar:vertical .thumb{" +
                        "-fx-background-color:#3C3F41;-fx-background-radius:3;}" +
                        ".scroll-bar:horizontal .thumb:hover,.scroll-bar:vertical .thumb:hover{" +
                        "-fx-background-color:#4C5052;-fx-background-radius:3;}" +
                        ".scroll-bar .increment-button,.scroll-bar .decrement-button{" +
                        "-fx-background-color:transparent;-fx-border-color:transparent;-fx-padding:0;}" +
                        ".scroll-bar .increment-arrow,.scroll-bar .decrement-arrow{" +
                        "-fx-shape:' ';-fx-padding:0;}" +
                        ".corner{-fx-background-color:#1E1F22;}";
        DARK_SCROLL_CSS = "data:text/css;base64," +
                Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    private CPUState cpuState;
    private final Map<String, Long> lastValues = new HashMap<>();
    private final Set<String> changedRegs = new HashSet<>();
    private final Map<String, VBox> cardNodes = new HashMap<>();
    private final Map<String, Label> valueLabelMap = new HashMap<>();
    private final Map<String, Label> expandBtnMap = new HashMap<>();
    private final FlowPane cardPane;
    private Popup expandPopup;
    private String expandedReg;
    private boolean popupShowDec = false;

    private Label[] popupBitCells;
    private long popupCurrentValue;
    private long popupLastHiddenTime = 0;
    private long lastToggleTime = 0;

    public RegistersView(CPUState initialState) {
        this.cpuState = initialState;

        cardPane = new FlowPane(8, 8);
        cardPane.setPadding(new Insets(10, 10, 10, 10));
        cardPane.setAlignment(Pos.TOP_LEFT);

        for (String reg : REG_INFO.keySet()) {
            VBox card = buildCard(reg);
            cardNodes.put(reg, card);
            cardPane.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(cardPane);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                " -fx-border-color: transparent;");
        scroll.getStylesheets().add(DARK_SCROLL_CSS);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
        setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(this, Priority.ALWAYS);

        render(false, null);
    }

    public void update(CPUState state, Set<String> involvedRegs) {
        this.cpuState = state;
        render(true, involvedRegs);
    }

    public void reset(CPUState state) {
        this.cpuState = state;
        lastValues.clear();
        changedRegs.clear();
        dismissPopup();
        render(false, null);
    }

    private void render(boolean detectChanges, Set<String> involvedRegs) {
        if (cpuState == null) return;

        if (detectChanges) {
            changedRegs.clear();
            for (String reg : REG_INFO.keySet()) {
                long current = cpuState.getRegister(reg, 8);
                Long prev = lastValues.get(reg);
                if (prev != null && prev != current) {
                    changedRegs.add(reg);
                }
            }

            if (involvedRegs != null) {
                changedRegs.addAll(involvedRegs);
            }
        } else {
            changedRegs.clear();
        }

        for (String reg : REG_INFO.keySet()) {
            long val = cpuState.getRegister(reg, 8);
            lastValues.put(reg, val);
            valueLabelMap.get(reg).setText(formatHex(val));
            styleCard(reg, changedRegs.contains(reg));
        }

        if (expandedReg != null && expandPopup != null && expandPopup.isShowing()) {
            refreshPopupContent();
        }
    }

    // ── Card building ─────────────────────────────────────────────────────────

    private VBox buildCard(String reg) {
        Label nameLabel = new Label("%" + reg);
        nameLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        Label valLabel = new Label("0x0");
        valLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        valueLabelMap.put(reg, valLabel);

        Label expandBtn = new Label("\u25B8 Expand");
        expandBtn.setStyle(expandBtnStyle(false));
        expandBtn.setOnMouseEntered(e -> expandBtn.setStyle(expandBtnStyle(true)));
        expandBtn.setOnMouseExited(e -> expandBtn.setStyle(expandBtnStyle(false)));
        expandBtn.setOnMousePressed(e -> {
            e.consume();
            toggleExpand(reg);
        });
        expandBtnMap.put(reg, expandBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(4, nameLabel, spacer, expandBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(3, topRow, valLabel);
        card.setPadding(new Insets(7, 10, 7, 10));
        card.setPrefWidth(135);
        card.setMinWidth(118);
        card.setMaxWidth(165);
        styleCard(reg, false);

        card.setOnMouseEntered(e -> {
            if (!changedRegs.contains(reg)) {
                card.setStyle(cardStyle(true, false));
            }
        });
        card.setOnMouseExited(e -> styleCard(reg, changedRegs.contains(reg)));

        return card;
    }

    private String expandBtnStyle(boolean hover) {
        return "-fx-font-family: " + SANS + ";" +
                "-fx-font-size: 9;" +
                "-fx-text-fill: " + (hover ? AMBER : TEXT_MUTED) + ";" +
                "-fx-cursor: hand;" +
                "-fx-padding: 1 5 1 5;" +
                "-fx-background-color: " + (hover ? "#3A3215" : "transparent") + ";" +
                "-fx-background-radius: 3;" +
                "-fx-border-color: " + (hover ? "#5A4010" : "transparent") + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 3;";
    }

    private void styleCard(String reg, boolean changed) {
        VBox card = cardNodes.get(reg);
        if (card != null) {
            card.setStyle(cardStyle(false, changed));
        }
    }

    private String cardStyle(boolean hover, boolean changed) {
        String bg = changed ? CHANGED_BG : (hover ? BG_HOVER : BG_RAISED);
        String border = changed ? CHANGED_BDR : BORDER_SOFT;
        return "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;";
    }

    // ── Expand / Popup ────────────────────────────────────────────────────────

    private void toggleExpand(String reg) {
        long now = System.currentTimeMillis();
        if (now - lastToggleTime < 250) {
            return;
        }
        lastToggleTime = now;
        if (expandedReg != null && expandedReg.equals(reg) && expandPopup != null && expandPopup.isShowing()) {
            dismissPopup();
            return;
        }

        if (System.currentTimeMillis() - popupLastHiddenTime < 300) {
            return;
        }

        dismissPopup();
        expandedReg = reg;
        popupShowDec = false;
        showExpandPopup(reg);
        updateExpandButtons();
    }

    private void dismissPopup() {
        if (expandPopup != null) {
            expandPopup.hide();
            expandPopup = null;
        }
        expandedReg = null;
        popupBitCells = null;
        updateExpandButtons();
    }

    private void updateExpandButtons() {
        for (Map.Entry<String, Label> entry : expandBtnMap.entrySet()) {
            boolean isExpanded = entry.getKey().equals(expandedReg);
            entry.getValue().setText(isExpanded ? "\u25BE Collapse" : "\u25B8 Expand");
        }
    }

    private void showExpandPopup(String reg) {
        VBox content = buildPopupContent(reg);

        expandPopup = new Popup();
        expandPopup.setAutoHide(true);
        expandPopup.getContent().add(content);
        expandPopup.setOnHidden(e -> {
            expandedReg = null;
            popupBitCells = null;
            popupLastHiddenTime = System.currentTimeMillis();
            updateExpandButtons();
        });

        VBox card = cardNodes.get(reg);
        if (card != null && card.getScene() != null && card.getScene().getWindow() != null) {
            Bounds screenBounds = card.localToScreen(card.getBoundsInLocal());
            if (screenBounds != null) {
                double popupX = screenBounds.getMinX();
                double popupY = screenBounds.getMaxY() + 4;
                expandPopup.show(card.getScene().getWindow(), popupX, popupY);
            }
        }
    }

    private void refreshPopupContent() {
        if (expandPopup == null || expandedReg == null) return;
        VBox content = buildPopupContent(expandedReg);
        expandPopup.getContent().clear();
        expandPopup.getContent().add(content);
    }

    // ── Popup content ─────────────────────────────────────────────────────────

    private VBox buildPopupContent(String reg) {
        long fullVal = cpuState.getRegister(reg, 8);
        popupCurrentValue = fullVal;
        SubRegInfo sub = REG_INFO.get(reg);

        long val32 = fullVal & 0xFFFFFFFFL;
        long val16 = fullVal & 0xFFFFL;
        long val8  = fullVal & 0xFFL;

        popupBitCells = new Label[64];

        Label regName = new Label("%" + reg);
        regName.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        Label eqSign = new Label("  =  ");
        eqSign.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        Label fullValueLabel = new Label(popupShowDec ? formatDec64(fullVal) : formatHex64(fullVal));
        fullValueLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_BRIGHT + ";"
        );

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Label fmtToggle = new Label(popupShowDec ? "HEX" : "DEC");
        fmtToggle.setPadding(new Insets(2, 6, 2, 6));
        fmtToggle.setStyle(toggleBtnStyle(false));
        fmtToggle.setOnMouseEntered(e -> fmtToggle.setStyle(toggleBtnStyle(true)));
        fmtToggle.setOnMouseExited(e -> fmtToggle.setStyle(toggleBtnStyle(false)));
        fmtToggle.setOnMouseClicked(e -> {
            popupShowDec = !popupShowDec;
            refreshPopupContent();
        });

        HBox titleRow = new HBox(0, regName, eqSign, fullValueLabel, titleSpacer, fmtToggle);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label bitsTitle = new Label("BIT-LEVEL VIEW");
        bitsTitle.setStyle(sectionLabelStyle());
        bitsTitle.setPadding(new Insets(4, 0, 2, 0));

        VBox bitsSection = buildBitsSection(fullVal);

        Label hintLabel = new Label("\u2139  Hover a sub-register below to highlight its bits");
        hintLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-font-style: italic;"
        );
        hintLabel.setPadding(new Insets(2, 0, 0, 0));

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

        Label subTitle = new Label("SUB-REGISTERS");
        subTitle.setStyle(sectionLabelStyle());
        subTitle.setPadding(new Insets(4, 0, 2, 0));

        VBox subRegs = new VBox(2);
        subRegs.getChildren().add(buildInteractiveSubRegRow(
                sub.name32, val32, 4, BIT_EAX, "bits 31\u20130", 31, 0));
        subRegs.getChildren().add(buildInteractiveSubRegRow(
                sub.name16, val16, 2, BIT_AX, "bits 15\u20130", 15, 0));
        subRegs.getChildren().add(buildInteractiveSubRegRow(
                sub.name8, val8, 1, BIT_AL, "bits 7\u20130", 7, 0));

        VBox popup = new VBox(4, titleRow, bitsTitle, bitsSection, hintLabel, divider, subTitle, subRegs);
        popup.setPadding(new Insets(10, 14, 10, 14));
        popup.setStyle(
                "-fx-background-color: #252729;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0.12, 0, 6);"
        );
        return popup;
    }

    // ── Bit-level section ─────────────────────────────────────────────────────

    private VBox buildBitsSection(long value) {
        VBox section = new VBox(1);
        section.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().add(buildByteBoundaryLabels(63, 32));
        section.getChildren().add(buildBitRow(value, 63, 32));

        Region gap = new Region();
        gap.setPrefHeight(4);
        section.getChildren().add(gap);

        section.getChildren().add(buildByteBoundaryLabels(31, 0));
        section.getChildren().add(buildBitRow(value, 31, 0));
        section.getChildren().add(buildSubRegBar(31, 0));

        return section;
    }

    private HBox buildByteBoundaryLabels(int highBit, int lowBit) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);

        int byteGroups = (highBit - lowBit + 1) / 8;

        for (int g = 0; g < byteGroups; g++) {
            int byteHighBit = highBit - g * 8;
            int byteLowBit = byteHighBit - 7;

            Label high = new Label(String.valueOf(byteHighBit));
            high.setStyle(byteIndexStyle());
            high.setMinWidth(20);
            high.setAlignment(Pos.CENTER_LEFT);

            Region mid = new Region();
            HBox.setHgrow(mid, Priority.ALWAYS);

            Label low = new Label(String.valueOf(byteLowBit));
            low.setStyle(byteIndexStyle());
            low.setMinWidth(20);
            low.setAlignment(Pos.CENTER_RIGHT);

            HBox byteLabel = new HBox(0, high, mid, low);
            double byteWidth = 8 * 15 + 7;
            byteLabel.setPrefWidth(byteWidth);
            byteLabel.setMinWidth(byteWidth);

            if (g < byteGroups - 1) {
                Region spacer = new Region();
                spacer.setPrefWidth(3);
                spacer.setMinWidth(3);
                row.getChildren().addAll(byteLabel, spacer);
            } else {
                row.getChildren().add(byteLabel);
            }
        }
        return row;
    }

    private HBox buildBitRow(long value, int highBit, int lowBit) {
        HBox row = new HBox(1);
        row.setAlignment(Pos.CENTER_LEFT);

        for (int i = highBit; i >= lowBit; i--) {
            int bit = (int) ((value >> i) & 1);
            Label cell = new Label(String.valueOf(bit));
            cell.setAlignment(Pos.CENTER);
            cell.setStyle(bitCellStyle(i, bit, false));
            popupBitCells[i] = cell;
            row.getChildren().add(cell);

            if (i > lowBit && (i - lowBit) % 8 == 0) {
                Region byteGap = new Region();
                byteGap.setPrefWidth(3);
                byteGap.setMinWidth(3);
                row.getChildren().add(byteGap);
            }
        }
        return row;
    }

    private HBox buildSubRegBar(int highBit, int lowBit) {
        HBox bar = new HBox(1);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(1, 0, 0, 0));

        for (int i = highBit; i >= lowBit; i--) {
            String color;
            if (i < 8)       color = BIT_AL;
            else if (i < 16) color = BIT_AX;
            else if (i < 32) color = BIT_EAX;
            else              color = "transparent";

            Region tick = new Region();
            tick.setPrefSize(15, 3);
            tick.setMinSize(15, 3);
            tick.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 1;");
            bar.getChildren().add(tick);

            if (i > lowBit && (i - lowBit) % 8 == 0) {
                Region gap = new Region();
                gap.setPrefWidth(3);
                gap.setMinWidth(3);
                bar.getChildren().add(gap);
            }
        }
        return bar;
    }

    // ── Interactive sub-register rows ─────────────────────────────────────────

    private HBox buildInteractiveSubRegRow(String name, long value, int bytes,
                                           String colorHint, String bitRange,
                                           int highBitIdx, int lowBitIdx) {
        Label colorBar = new Label();
        colorBar.setPrefSize(4, 20);
        colorBar.setMinSize(4, 20);
        colorBar.setStyle(
                "-fx-background-color: " + colorHint + ";" +
                        "-fx-background-radius: 2;"
        );

        Label nameLabel = new Label("%" + name);
        nameLabel.setMinWidth(52);
        nameLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + BLUE + ";"
        );

        String valStr = popupShowDec
                ? formatDecSigned(value, bytes) + "  (" + formatSubHex(value, bytes) + ")"
                : formatSubHex(value, bytes) + "  (" + formatDecSigned(value, bytes) + ")";

        Label valueLabel = new Label("= " + valStr);
        valueLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rangeLabel = new Label(bitRange);
        rangeLabel.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );

        HBox row = new HBox(6, colorBar, nameLabel, valueLabel, spacer, rangeLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 8, 3, 6));
        row.setStyle(subRegRowStyle(false));

        row.setOnMouseEntered(e -> {
            row.setStyle(subRegRowStyle(true));
            highlightBits(highBitIdx, lowBitIdx, true);
        });
        row.setOnMouseExited(e -> {
            row.setStyle(subRegRowStyle(false));
            highlightBits(highBitIdx, lowBitIdx, false);
        });

        return row;
    }

    private void highlightBits(int highBit, int lowBit, boolean highlight) {
        if (popupBitCells == null) return;
        for (int i = lowBit; i <= highBit && i < 64; i++) {
            Label cell = popupBitCells[i];
            if (cell == null) continue;
            int bit = (int) ((popupCurrentValue >> i) & 1);
            cell.setStyle(bitCellStyle(i, bit, highlight));
        }
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private String bitCellStyle(int bitIdx, int bitVal, boolean highlighted) {
        String bg;
        if (bitIdx < 8)       bg = BIT_AL;
        else if (bitIdx < 16) bg = BIT_AX;
        else if (bitIdx < 32) bg = BIT_EAX;
        else                  bg = BIT_DEFAULT;

        String textColor = (bitVal == 1) ? BIT_ONE_TEXT : BIT_ZERO_TEXT;
        String border = BIT_BORDER;

        if (highlighted) {
            bg = HIGHLIGHT_GLOW;
            border = HIGHLIGHT_BORDER;
            textColor = TEXT_BRIGHT;
        }

        return "-fx-font-family: " + MONO + ";" +
                "-fx-font-size: 9.5;" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 0.5;" +
                "-fx-alignment: center;" +
                "-fx-pref-width: 15;" +
                "-fx-min-width: 15;" +
                "-fx-pref-height: 18;" +
                "-fx-min-height: 18;";
    }

    private String subRegRowStyle(boolean hovered) {
        if (hovered) {
            return "-fx-background-color: #1A2E45;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: #2A4A6A;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;";
        }
        return "-fx-background-color: transparent;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: transparent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;";
    }

    private String toggleBtnStyle(boolean hover) {
        return "-fx-font-family: " + SANS + ";" +
                "-fx-font-size: 9;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + (hover ? TEXT_BRIGHT : TEXT_MUTED) + ";" +
                "-fx-background-color: " + (hover ? BG_HOVER : BG_RAISED) + ";" +
                "-fx-background-radius: 3;" +
                "-fx-border-color: " + BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 3;" +
                "-fx-cursor: hand;" +
                "-fx-letter-spacing: 0.5;";
    }

    private String sectionLabelStyle() {
        return "-fx-font-family: " + SANS + ";" +
                "-fx-font-size: 8.5;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-letter-spacing: 1;";
    }

    private String byteIndexStyle() {
        return "-fx-font-family: " + MONO + ";" +
                "-fx-font-size: 8;" +
                "-fx-text-fill: " + TEXT_MUTED + ";";
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private static String formatHex(long v) {
        if (v == 0) return "0x0";
        return "0x" + Long.toHexString(v).toUpperCase();
    }

    private static String formatHex64(long v) {
        return "0x" + String.format("%016X", v);
    }

    private static String formatDec64(long v) {
        return Long.toString(v);
    }

    private static String formatSubHex(long v, int bytes) {
        String fmt = switch (bytes) {
            case 1 -> "%02X";
            case 2 -> "%04X";
            case 4 -> "%08X";
            default -> "%016X";
        };
        return "0x" + String.format(fmt, v);
    }

    private static String formatDecSigned(long v, int bytes) {
        long signed = switch (bytes) {
            case 1 -> (byte) v;
            case 2 -> (short) v;
            case 4 -> (int) v;
            default -> v;
        };
        return Long.toString(signed);
    }
}