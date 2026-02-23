package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

// DISCLAIMER: This class was written largely with the help of LLMs
public class StackView extends VBox {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final String BG_RAISED    = "#3C3F41";
    private static final String BG_HOVER     = "#4C5052";
    private static final String BORDER_SOFT  = "#424547";
    private static final String AMBER        = "#E8A845";
    private static final String TEXT_PRIMARY = "#BBBBBB";
    private static final String TEXT_BRIGHT  = "#E8E8E8";
    private static final String TEXT_MUTED   = "#777777";
    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    // Row backgrounds — pointer states have priority
    private static final String ROW_EVEN     = "transparent";
    private static final String ROW_ODD      = "rgba(255,255,255,0.025)";
    private static final String ROW_RSP      = "#0A1E38";
    private static final String ROW_RBP      = "#0A2014";
    private static final String ROW_BOTH     = "#1A0D30";
    private static final String ROW_CHANGED  = "#2B1E00";

    // Pointer badge colours
    private static final String RSP_FG  = "#56A6E8";
    private static final String RSP_BG  = "#0D2540";
    private static final String RSP_BDR = "#1B4F80";
    private static final String RBP_FG  = "#6A8759";
    private static final String RBP_BG  = "#0D2A14";
    private static final String RBP_BDR = "#1D5C30";
    private static final String BTH_FG  = "#C09AFF";
    private static final String BTH_BG  = "#2A1040";
    private static final String BTH_BDR = "#5A3090";

    private static final int ROWS = 10;

    // ── State ─────────────────────────────────────────────────────────────────
    private CPUState cpuState;
    private boolean  reversed = false;

    /** Data indices (not display rows) whose value changed on the last step. */
    private final Set<Integer> changedIdx = new HashSet<>();
    private Long[] lastValues = new Long[ROWS];

    // ── Widgets ───────────────────────────────────────────────────────────────
    private final HBox[]  rows       = new HBox[ROWS];
    private final HBox[]  badgeBoxes = new HBox[ROWS];
    private final Label[] addrLabels = new Label[ROWS];
    private final Label[] valLabels  = new Label[ROWS];
    private final Label   flipLabel;
    private final Label   addrModeLabel;
    private final Label   valModeLabel;
    private Label   valHdrLabel;

    private boolean addrRelative = true;
    private boolean valHex       = true;

    // ── Constructor ───────────────────────────────────────────────────────────

    public StackView(CPUState initialState) {
        this.cpuState = initialState;
        setSpacing(0);
        setFillWidth(true);

        Button upBtn   = makeBtn("↑  Up");
        Button downBtn = makeBtn("↓  Down");
        flipLabel = makeToggleLbl("⇅  Flip");
        flipLabel.setOnMouseClicked(e -> toggleDirection());
        flipLabel.setOnMouseEntered(e -> styleLbl(flipLabel, true));
        flipLabel.setOnMouseExited(e  -> styleLbl(flipLabel, false));
        upBtn.setOnAction(e   -> scrollManual(+1));
        downBtn.setOnAction(e -> scrollManual(-1));

        addrModeLabel = makeToggleLbl("Addr: Relative");
        addrModeLabel.setOnMouseClicked(e -> toggleAddrMode());
        addrModeLabel.setOnMouseEntered(e -> styleLbl(addrModeLabel, true));
        addrModeLabel.setOnMouseExited(e  -> styleLbl(addrModeLabel, false));

        valModeLabel = makeToggleLbl("Value: Hex");
        valModeLabel.setOnMouseClicked(e -> toggleValMode());
        valModeLabel.setOnMouseEntered(e -> styleLbl(valModeLabel, true));
        valModeLabel.setOnMouseExited(e  -> styleLbl(valModeLabel, false));

        upBtn.setMaxWidth(Double.MAX_VALUE);
        downBtn.setMaxWidth(Double.MAX_VALUE);
        flipLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(upBtn,     Priority.ALWAYS);
        HBox.setHgrow(flipLabel, Priority.ALWAYS);
        HBox.setHgrow(downBtn,   Priority.ALWAYS);
        HBox row1 = new HBox(4, upBtn, flipLabel, downBtn);
        row1.setPadding(new Insets(8, 10, 4, 10));
        row1.setStyle("-fx-background-color: " + BG_RAISED + ";");

        addrModeLabel.setMaxWidth(Double.MAX_VALUE);
        valModeLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addrModeLabel, Priority.ALWAYS);
        HBox.setHgrow(valModeLabel,  Priority.ALWAYS);
        HBox row2 = new HBox(4, addrModeLabel, valModeLabel);
        row2.setPadding(new Insets(0, 10, 8, 10));
        row2.setStyle("-fx-background-color: " + BG_RAISED + ";");

        VBox table = new VBox(0);
        table.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getChildren().add(buildHeader());
        for (int i = 0; i < ROWS; i++) {
            rows[i] = buildRow(i);
            table.getChildren().add(rows[i]);
        }

        getChildren().addAll(row1, row2, table, buildLegend());
        render(false);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void update(CPUState state) {
        this.cpuState = state;
        autoScrollToRsp();
        render(true);
    }

    public void reset(CPUState state) {
        this.cpuState = state;
        changedIdx.clear();
        lastValues = new Long[ROWS];
        render(false);
    }

    // ── Auto-scroll ───────────────────────────────────────────────────────────

    /**
     * If RSP has moved outside the visible 10-row window, snap the window so
     * RSP lands on the boundary row that it crossed (top or bottom).
     * Only fires when RSP is genuinely out of the window — manual scrolls
     * that keep RSP inside are never disturbed.
     */
    private void autoScrollToRsp() {
        if (cpuState == null) return;
        Memory mem     = cpuState.getMemory();
        long rsp       = cpuState.getRegister("rsp", 8);
        long viewStart = mem.getStackViewStart();               // highest (top row) address
        long viewEnd   = viewStart - (long)(ROWS - 1) * 8;     // lowest (bottom row) address

        if (rsp > viewStart) {
            // RSP went above the top → snap so RSP is the top row
            mem.setStackViewStart(rsp);
            lastValues = new Long[ROWS]; // window moved; invalidate old diff
        } else if (rsp < viewEnd) {
            // RSP went below the bottom → snap so RSP is the bottom row
            mem.setStackViewStart(rsp + (long)(ROWS - 1) * 8);
            lastValues = new Long[ROWS];
        }
        // If RSP is inside [viewEnd, viewStart], leave the window alone.
    }

    // ── Manual scroll ─────────────────────────────────────────────────────────

    /**
     * direction +1 = Up button (show higher addresses → increase viewStart)
     * direction -1 = Down button (show lower addresses → decrease viewStart)
     */
    private void scrollManual(int direction) {
        if (cpuState == null) return;
        Memory mem = cpuState.getMemory();
        mem.setStackViewStart(mem.getStackViewStart() + direction * 8L);
        lastValues = new Long[ROWS];
        changedIdx.clear();
        render(false);
    }

    private void toggleDirection() {
        reversed = !reversed;
        flipLabel.setText(reversed ? "⇅  Normal" : "⇅  Flip");
        lastValues = new Long[ROWS];
        changedIdx.clear();
        render(false);
    }

    // ── Core render ───────────────────────────────────────────────────────────

    private void render(boolean detectChanges) {
        if (cpuState == null) return;

        Memory mem     = cpuState.getMemory();
        Long[] values  = mem.getStack();
        long viewStart = mem.getStackViewStart();
        long rsp       = cpuState.getRegister("rsp", 8);
        long rbp       = cpuState.getRegister("rbp", 8);

        if (detectChanges) {
            changedIdx.clear();
            for (int i = 0; i < ROWS; i++) {
                if (lastValues[i] != null && !lastValues[i].equals(values[i])) {
                    changedIdx.add(i);
                }
            }
        }

        for (int dr = 0; dr < ROWS; dr++) {
            // dataIdx is the index into values[] / lastValues[]
            int  di      = reversed ? (ROWS - 1 - dr) : dr;
            long addr    = viewStart - (long) di * 8;
            long val     = values[di];
            boolean isRsp     = (addr == rsp);
            boolean isRbp     = (addr == rbp);
            boolean isChanged = changedIdx.contains(di);

            addrLabels[dr].setText(addrRelative ? relAddr(addr, rbp) : rawAddr(addr));
            setValueLabel(valLabels[dr], val, isChanged);
            setBadges(badgeBoxes[dr], isRsp, isRbp);
            setRowBg(dr, isRsp, isRbp, isChanged);
        }

        System.arraycopy(values, 0, lastValues, 0, ROWS);
    }

    // ── Row styling ───────────────────────────────────────────────────────────

    private void setRowBg(int dr, boolean isRsp, boolean isRbp, boolean isChanged) {
        String bg;
        if      (isRsp && isRbp) bg = ROW_BOTH;
        else if (isRsp)          bg = ROW_RSP;
        else if (isRbp)          bg = ROW_RBP;
        else if (isChanged)      bg = ROW_CHANGED;
        else                     bg = (dr % 2 == 0) ? ROW_EVEN : ROW_ODD;
        rows[dr].setStyle("-fx-background-color: " + bg + ";");
    }

    private void setValueLabel(Label lbl, long val, boolean changed) {
        lbl.setText(valHex ? formatHex(val) : formatDec(val));
        if (changed) {
            lbl.setStyle(
                    "-fx-font-family: " + MONO + ";" +
                            "-fx-font-size: 12;" +
                            "-fx-text-fill: " + AMBER + ";" +
                            "-fx-font-weight: bold;"
            );
        } else {
            lbl.setStyle(
                    "-fx-font-family: " + MONO + ";" +
                            "-fx-font-size: 12;" +
                            "-fx-text-fill: " + TEXT_PRIMARY + ";"
            );
        }
    }

    private void setBadges(HBox box, boolean isRsp, boolean isRbp) {
        box.getChildren().clear();
        if (isRsp && isRbp) {
            box.getChildren().addAll(badge("RSP", BTH_FG, BTH_BG, BTH_BDR),
                    badge("RBP", BTH_FG, BTH_BG, BTH_BDR));
        } else if (isRsp) {
            box.getChildren().add(badge("RSP", RSP_FG, RSP_BG, RSP_BDR));
        } else if (isRbp) {
            box.getChildren().add(badge("RBP", RBP_FG, RBP_BG, RBP_BDR));
        }
    }

    // ── Widget builders ───────────────────────────────────────────────────────

    private HBox buildRow(int idx) {
        HBox box = new HBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(62);
        box.setMinWidth(62);
        badgeBoxes[idx] = box;

        Label addr = new Label("—");
        addr.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-pref-width: 78;" +
                        "-fx-min-width: 78;"
        );
        addrLabels[idx] = addr;

        Label sep = new Label("│");
        sep.setStyle("-fx-text-fill: " + BORDER_SOFT + "; -fx-font-size: 11;");

        Label val = new Label("—");
        val.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        HBox.setHgrow(val, Priority.ALWAYS);
        valLabels[idx] = val;

        HBox row = new HBox(6, box, addr, sep, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setMinHeight(28);
        row.setStyle("-fx-background-color: " + ((idx % 2 == 0) ? ROW_EVEN : ROW_ODD) + ";");
        VBox.setVgrow(row, Priority.ALWAYS);
        return row;
    }

    private HBox buildHeader() {
        Label ptr  = hdr("PTR",     62);
        Label addr = hdr("ADDRESS", 72);
        Label sep  = new Label("  ");
        valHdrLabel = new Label("VALUE (hex)");
        valHdrLabel.setStyle(colHdrStyle());
        HBox.setHgrow(valHdrLabel, Priority.ALWAYS);

        HBox h = new HBox(6, ptr, addr, sep, valHdrLabel);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(3, 10, 3, 10));
        h.setStyle(
                "-fx-background-color: #272829;" +
                        "-fx-border-color: transparent transparent " + BORDER_SOFT + " transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        return h;
    }

    private HBox buildLegend() {
        HBox leg = new HBox(10);
        leg.setAlignment(Pos.CENTER_LEFT);
        leg.setPadding(new Insets(6, 10, 6, 10));
        leg.setStyle("-fx-background-color: #272829;");
        leg.getChildren().addAll(
                legendEntry("RSP",     RSP_FG, RSP_BG, RSP_BDR),
                legendEntry("RBP",     RBP_FG, RBP_BG, RBP_BDR),
                legendEntry("RSP+RBP", BTH_FG, BTH_BG, BTH_BDR)
        );
        return leg;
    }

    private HBox legendEntry(String text, String fg, String bg, String bdr) {
        Label b = badge(text, fg, bg, bdr);
        Label d = new Label("= " + text);
        d.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 9;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );
        HBox e = new HBox(4, b, d);
        e.setAlignment(Pos.CENTER_LEFT);
        return e;
    }

    private Label badge(String text, String fg, String bg, String bdr) {
        Label b = new Label(text);
        b.setPadding(new Insets(1, 4, 1, 4));
        b.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 3;" +
                        "-fx-border-color: " + bdr + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 3;" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 8.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + fg + ";"
        );
        return b;
    }

    private Label hdr(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setMinWidth(w);
        l.setStyle(colHdrStyle());
        return l;
    }

    private String colHdrStyle() {
        return "-fx-font-family: " + SANS + ";" +
                "-fx-font-size: 9;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-letter-spacing: 0.8;";
    }

    // ── Control styling ───────────────────────────────────────────────────────

    private Button makeBtn(String text) {
        Button b = new Button(text);
        b.setStyle(btnStyle(false));
        b.setOnMouseEntered(e -> b.setStyle(btnStyle(true)));
        b.setOnMouseExited(e  -> b.setStyle(btnStyle(false)));
        return b;
    }

    private String btnStyle(boolean hover) {
        return "-fx-background-color: " + (hover ? BG_HOVER : "#2A2C2E") + ";" +
                "-fx-text-fill: " + (hover ? TEXT_BRIGHT : TEXT_PRIMARY) + ";" +
                "-fx-font-family: " + SANS + ";" +
                "-fx-font-size: 10.5;" +
                "-fx-padding: 4 10 4 10;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: " + BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;";
    }

    private void styleLbl(Label l, boolean hover) {
        l.setStyle(
                "-fx-background-color: " + (hover ? BG_HOVER : "#2A2C2E") + ";" +
                        "-fx-text-fill: " + (hover ? TEXT_BRIGHT : TEXT_PRIMARY) + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-cursor: hand;"
        );
    }

    private Label makeToggleLbl(String text) {
        Label l = new Label(text);
        l.setPadding(new Insets(5, 10, 5, 10));
        l.setAlignment(Pos.CENTER);
        styleLbl(l, false);
        return l;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private void toggleAddrMode() {
        addrRelative = !addrRelative;
        addrModeLabel.setText(addrRelative ? "Addr: Relative" : "Addr: Raw");
        render(false);
    }

    private void toggleValMode() {
        valHex = !valHex;
        valModeLabel.setText(valHex ? "Value: Hex" : "Value: Dec");
        valHdrLabel.setText(valHex ? "VALUE (hex)" : "VALUE (dec)");
        render(false);
    }

    private static String relAddr(long addr, long rbp) {
        long diff = addr - rbp;
        if (diff == 0) return "rbp";
        if (diff > 0)  return "rbp+" + diff;
        return "rbp" + diff;
    }

    private static String rawAddr(long addr) {
        String s = String.format("%016X", addr);
        return "…" + s.substring(s.length() - 7);
    }

    private static String formatHex(long v) {
        if (v == 0) return "0x0";
        return "0x" + Long.toHexString(v).toUpperCase();
    }

    private static String formatDec(long v) {
        // Stack values are stored as unsigned 64-bit, but display signed for readability
        return Long.toString(v);
    }
}