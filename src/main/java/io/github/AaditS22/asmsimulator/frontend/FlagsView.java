package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.Flags;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// DISCLAIMER: This class was written largely with the help of LLMs
public class FlagsView extends VBox {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String AMBER           = "#E8A845";
    private static final String TEXT_MUTED      = "#606468";
    private static final String TEXT_DIM        = "#4A4D50";
    private static final String BORDER_SOFT     = "#333537";
    private static final String CHIP_OFF_BG     = "#252729";
    private static final String CHIP_OFF_BORDER = "#353739";
    private static final String CHIP_ON_BG      = "#1A3820";
    private static final String CHIP_ON_BORDER  = "#3A7A3A";
    private static final String CHIP_ON_TEXT    = "#6ADA6A";

    // Colours used inside the TextFlow description
    private static final String HIGHLIGHT_BLUE  = "#6AADDA";  // key concepts
    private static final String HIGHLIGHT_AMBER = "#D4924A";  // flag/instruction names
    private static final String DESC_BASE       = "#8A8F94";  // normal prose

    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

    // ── Flag definitions: { acronym, full name } ─────────────────────────────
    private static final String[][] FLAG_DEFS = {
            {"ZF", "Zero"},
            {"CF", "Carry"},
            {"SF", "Sign"},
            {"OF", "Overflow"},
    };

    // ── State ────────────────────────────────────────────────────────────────
    private CPUState cpuState;
    private final Map<String, Label>   chipMap      = new HashMap<>();
    private final Map<String, Boolean> lastValues   = new HashMap<>();
    private final Set<String>          changedFlags = new HashSet<>();
    private final TextFlow             descFlow;

    // ── Constructor ──────────────────────────────────────────────────────────
    public FlagsView(CPUState initialState) {
        this.cpuState = initialState;
        setPadding(new Insets(12, 14, 14, 14));
        setSpacing(0);
        setAlignment(Pos.TOP_LEFT);
        setStyle("-fx-background-color: transparent;");

        for (String[] def : FLAG_DEFS) {
            String acronym  = def[0];
            String fullName = def[1];

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER);
            row.setPadding(new Insets(9, 0, 9, 0));

            Label acronymLabel = new Label(acronym);
            acronymLabel.setStyle(
                    "-fx-font-family: " + MONO + ";" +
                            "-fx-font-size: 11.5;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: " + AMBER + ";"
            );

            Label fullLabel = new Label(fullName);
            fullLabel.setStyle(
                    "-fx-font-family: " + SANS + ";" +
                            "-fx-font-size: 10.5;" +
                            "-fx-text-fill: " + TEXT_MUTED + ";" +
                            "-fx-padding: 1 0 0 0;"
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label chip = new Label("0");
            chip.setPadding(new Insets(2, 9, 2, 9));
            chip.setStyle(chipStyle(false));
            chipMap.put(acronym, chip);

            row.getChildren().addAll(acronymLabel, fullLabel, spacer, chip);

            Region divider = new Region();
            divider.setPrefHeight(1);
            divider.setMaxHeight(1);
            divider.setStyle("-fx-background-color: " + BORDER_SOFT + ";");

            getChildren().addAll(row, divider);
        }

        // ── Description pane ─────────────────────────────────────────────────
        Region topGap = new Region();
        topGap.setPrefHeight(10);

        descFlow = new TextFlow();
        descFlow.setLineSpacing(4);
        descFlow.setPadding(new Insets(10, 2, 2, 2));
        descFlow.setStyle(
                "-fx-border-color: " + BORDER_SOFT + " transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;"
        );

        getChildren().addAll(topGap, descFlow);
        render(false);
    }

    public void update(CPUState state) {
        this.cpuState = state;
        render(true);
    }

    public void reset(CPUState state) {
        this.cpuState = state;
        lastValues.clear();
        changedFlags.clear();
        render(false);
    }

    // ── Render ───────────────────────────────────────────────────────────────
    private void render(boolean detectChanges) {
        if (cpuState == null) return;
        Flags flags = cpuState.getFlags();

        boolean zf = flags.isZero();
        boolean cf = flags.isCarry();
        boolean sf = flags.isNegative();
        boolean of = flags.isOverflow();

        Map<String, Boolean> current = Map.of("ZF", zf, "CF", cf, "SF", sf, "OF", of);

        if (detectChanges) {
            changedFlags.clear();
            for (Map.Entry<String, Boolean> e : current.entrySet()) {
                Boolean prev = lastValues.get(e.getKey());
                if (prev != null && !prev.equals(e.getValue())) changedFlags.add(e.getKey());
            }
        } else {
            changedFlags.clear();
        }

        for (String[] def : FLAG_DEFS) {
            String  acronym = def[0];
            boolean val     = current.get(acronym);
            boolean changed = changedFlags.contains(acronym);
            lastValues.put(acronym, val);

            Label chip = chipMap.get(acronym);
            if (chip != null) {
                chip.setText(val ? "1" : "0");
                chip.setStyle(chipStyle(changed));
            }
        }

        descFlow.getChildren().setAll(buildDescription(zf, cf, sf, of));
    }

    // ── Description builder ──────────────────────────────────────────────────
    /**
     * Produces a short, flowing, combination-aware description as a list of
     * individually-styled Text nodes. Key concepts are blue; flag/instruction
     * names are amber. Two sentences max — no bullet points.
     */
    private List<Text> buildDescription(boolean zf, boolean cf, boolean sf, boolean of) {
        Seg s = new Seg();

        // All clear
        if (!zf && !cf && !sf && !of) {
            return s.plain("All flags are clear. The last result was a ")
                    .hi("non-zero positive number")
                    .plain(" with no carry or overflow.")
                    .build();
        }

        // SF=1 + OF=1 — the classic signed overflow pair
        if (sf && of) {
            return s.plain("The result looks ")
                    .hi("negative")
                    .plain(", but ")
                    .hi("signed overflow")
                    .plain(" occurred — the true result was ")
                    .hi("too large to fit")
                    .plain(" and wrapped past the boundary, flipping the sign.")
                    .build();
        }

        // SF=0 + OF=1 — overflowed the other way
        if (!sf && of) {
            return s.plain("A ")
                    .hi("signed overflow")
                    .plain(" occurred. The true result was ")
                    .hi("negative")
                    .plain(" but wrapped to appear ")
                    .hi("positive")
                    .plain(" — the sign bit flipped, so the value cannot be trusted.")
                    .build();
        }

        // ZF=1 + CF=1
        if (zf && cf) {
            return s.plain("The result was ")
                    .hi("zero")
                    .plain(" and produced an ")
                    .hi("unsigned carry")
                    .plain(". This happens when two values sum to exactly the" +
                            " register's boundary, wrapping cleanly back to zero.")
                    .build();
        }

        // CF=1 + SF=1
        if (cf && sf) {
            return s.plain("The result is ")
                    .hi("negative")
                    .plain(" and an ")
                    .hi("unsigned borrow")
                    .plain(" occurred. After ")
                    .flag("cmp")
                    .plain(", this means the destination was ")
                    .hi("less than")
                    .plain(" the source (unsigned comparison).")
                    .build();
        }

        // ZF=1 only
        if (zf && !cf && !sf && !of) {
            return s.plain("The last result was ")
                    .hi("exactly zero")
                    .plain(". After ")
                    .flag("cmp")
                    .plain(", this means the two operands were ")
                    .hi("equal")
                    .plain(".")
                    .build();
        }

        // CF=1 only
        if (cf && !zf && !sf && !of) {
            return s.plain("An ")
                    .hi("unsigned carry or borrow")
                    .plain(" occurred — the result crossed the ")
                    .hi("unsigned boundary")
                    .plain(" of the register. After ")
                    .flag("cmp")
                    .plain(", the destination was ")
                    .hi("below")
                    .plain(" the source.")
                    .build();
        }

        // SF=1 only
        if (sf && !zf && !cf && !of) {
            return s.plain("The result is ")
                    .hi("negative")
                    .plain(" — its ")
                    .hi("most significant bit")
                    .plain(" is 1. No overflow occurred, so the signed value is correct.")
                    .build();
        }

        // ZF=1, SF=1 (unusual)
        if (zf && sf) {
            return s.plain("The result is ")
                    .hi("zero")
                    .plain(" with the ")
                    .hi("sign bit")
                    .plain(" set — an unusual combination. The zero result happened" +
                            " to set the MSB; double-check your instruction.")
                    .build();
        }

        // ZF=1, SF=1, OF=1
        if (zf && sf && of) {
            return s.plain("A ")
                    .hi("signed overflow")
                    .plain(" produced a result that appears ")
                    .hi("zero")
                    .plain(" with the ")
                    .hi("sign bit")
                    .plain(" set — a rare edge case where the value wrapped exactly to zero.")
                    .build();
        }

        // Fallback — list active flags
        List<String> active = new ArrayList<>();
        if (zf) active.add("ZF");
        if (cf) active.add("CF");
        if (sf) active.add("SF");
        if (of) active.add("OF");

        s.plain("Active: ");
        for (int i = 0; i < active.size(); i++) {
            s.flag(active.get(i));
            if (i < active.size() - 1) s.plain(", ");
        }
        s.plain(". This is an uncommon combination — check each flag individually.");
        return s.build();
    }

    // ── Text node factories ──────────────────────────────────────────────────
    private Text plainText(String content) {
        Text t = new Text(content);
        t.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-fill: " + DESC_BASE + ";"
        );
        return t;
    }

    private Text hiText(String content) {
        Text t = new Text(content);
        t.setStyle(
                "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-fill: " + HIGHLIGHT_BLUE + ";"
        );
        return t;
    }

    private Text flagText(String content) {
        Text t = new Text(content);
        t.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10;" +
                        "-fx-font-weight: bold;" +
                        "-fx-fill: " + HIGHLIGHT_AMBER + ";"
        );
        return t;
    }

    // ── Fluent builder for description segments ──────────────────────────────
    private class Seg {
        private final List<Text> nodes = new ArrayList<>();
        Seg plain(String s) { nodes.add(plainText(s)); return this; }
        Seg hi(String s)    { nodes.add(hiText(s));    return this; }
        Seg flag(String s)  { nodes.add(flagText(s));  return this; }
        List<Text> build()  { return nodes; }
    }

    // ── Chip style ───────────────────────────────────────────────────────────
    private String chipStyle(boolean changed) {
        String bg     = changed ? CHIP_ON_BG     : CHIP_OFF_BG;
        String border = changed ? CHIP_ON_BORDER : CHIP_OFF_BORDER;
        String text   = changed ? CHIP_ON_TEXT   : TEXT_DIM;

        return "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 3;" +
                "-fx-background-radius: 3;" +
                "-fx-font-family: " + MONO + ";" +
                "-fx-font-size: 11;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + text + ";";
    }
}