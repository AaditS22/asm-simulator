package io.github.AaditS22.asmsimulator.frontend;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.DataLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MemoryView extends VBox {

    private static final String BG_RAISED   = "#3C3F41";
    private static final String BG_HOVER    = "#4C5052";
    private static final String BORDER_SOFT = "#424547";
    private static final String AMBER       = "#E8A845";
    private static final String TEXT_PRIMARY = "#BBBBBB";
    private static final String TEXT_MUTED  = "#777777";
    private static final String CHANGED_BG  = "#2B1E00";
    private static final String CHANGED_BDR = "#5A4010";
    private static final String SANS = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif";
    private static final String MONO = "'JetBrains Mono', 'Consolas', 'Courier New', monospace";

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
    private LabelManager labelManager;
    private final VBox rowsContainer;
    private final Set<Long> manualAddresses = new HashSet<>();
    private final Map<Long, Long> lastValues = new HashMap<>();

    private final Map<Long, VBox> rowNodes = new HashMap<>();
    private final Map<Long, Label> valueLabelMap = new HashMap<>();
    private final Map<Long, Label> subtitleLabelMap = new HashMap<>();
    private final Set<Long> changedAddresses = new HashSet<>();

    public MemoryView(CPUState state, LabelManager labelManager) {
        this.cpuState = state;
        this.labelManager = labelManager;

        HBox searchBar = buildSearchBar();

        rowsContainer = new VBox(6);
        rowsContainer.setPadding(new Insets(10, 10, 10, 10));

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                " -fx-border-color: transparent;");
        scroll.getStylesheets().add(DARK_SCROLL_CSS);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(searchBar, scroll);
        setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(this, Priority.ALWAYS);

        render(false);
    }

    private HBox buildSearchBar() {
        TextField searchField = new TextField();
        searchField.setPromptText("Enter a memory address you want to track, in decimal/hex format (e.g. 0x4000)");
        searchField.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-background-radius: 4;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Track");
        searchBtn.setStyle(
                "-fx-background-color: " + BG_RAISED + ";" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-family: " + SANS + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-border-color: " + BORDER_SOFT + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;"
        );
        searchBtn.setOnMouseEntered(e -> searchBtn.setStyle(searchBtn.getStyle().replace(BG_RAISED, BG_HOVER)));
        searchBtn.setOnMouseExited(e -> searchBtn.setStyle(searchBtn.getStyle().replace(BG_HOVER, BG_RAISED)));

        searchBtn.setOnAction(e -> {
            String text = searchField.getText().trim();
            if (text.isEmpty()) return;
            try {
                long addr = text.startsWith("0x") ? Long.parseLong(text.substring(2), 16) : Long.parseLong(text);
                manualAddresses.add(addr);
                searchField.clear();
                render(false);
            } catch (NumberFormatException ex) {
                searchField.setText("Invalid format");
            }
        });

        HBox bar = new HBox(8, searchField, searchBtn);
        bar.setPadding(new Insets(10, 10, 0, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    public void update(CPUState state) {
        this.cpuState = state;
        render(true);
    }

    public void reset(CPUState state, LabelManager labelManager) {
        this.cpuState = state;
        this.labelManager = labelManager;
        lastValues.clear();
        manualAddresses.clear();
        changedAddresses.clear();
        rowNodes.clear();
        valueLabelMap.clear();
        subtitleLabelMap.clear();
        rowsContainer.getChildren().clear();
        render(false);
    }

    private void render(boolean detectChanges) {
        if (cpuState == null) return;

        Set<Long> addressesToShow = new TreeSet<>();
        Map<Long, String> labelNames = new HashMap<>();
        Map<Long, Integer> addressSizes = new HashMap<>();

        if (labelManager != null) {
            for (Map.Entry<String, DataLabel> entry : labelManager.getDataLabels().entrySet()) {
                long addr = entry.getValue().address();
                addressesToShow.add(addr);
                labelNames.put(addr, entry.getKey());
                addressSizes.put(addr, entry.getValue().size());
            }
        }

        addressesToShow.addAll(cpuState.getMemory().getAccessedAddresses());
        addressesToShow.addAll(manualAddresses);

        if (detectChanges) {
            changedAddresses.clear();
            for (Long addr : addressesToShow) {
                int size = addressSizes.getOrDefault(addr, 8);
                long current = cpuState.getMemory().readN(addr, size);
                Long prev = lastValues.get(addr);
                if (prev != null && prev != current) {
                    changedAddresses.add(addr);
                }
            }
        } else {
            changedAddresses.clear();
        }

        Map<Long, String> trackedReasons = cpuState.getMemory().getTrackedReasons();

        // Sort addresses: Changed ones first, then ascending by address
        List<Long> sortedAddresses = new ArrayList<>(addressesToShow);
        sortedAddresses.sort((a, b) -> {
            boolean changedA = changedAddresses.contains(a);
            boolean changedB = changedAddresses.contains(b);
            if (changedA && !changedB) return -1;
            if (!changedA && changedB) return 1;
            return Long.compare(a, b);
        });

        // Clear and re-add in sorted order so JavaFX handles the bubbling automatically
        rowsContainer.getChildren().clear();

        for (Long addr : sortedAddresses) {
            if (!rowNodes.containsKey(addr)) {
                String labelName = labelNames.get(addr);
                String reason = trackedReasons.get(addr);
                VBox card = buildRow(addr, labelName, reason);
                rowNodes.put(addr, card);
            }

            rowsContainer.getChildren().add(rowNodes.get(addr));

            int size = addressSizes.getOrDefault(addr, 8);
            long val = cpuState.getMemory().readN(addr, size);
            lastValues.put(addr, val);

            Label valLabel = valueLabelMap.get(addr);
            if (valLabel != null) {
                valLabel.setText(formatHex(val));
            }

            // Update the subtitle dynamically if a reason becomes available later
            if (!labelNames.containsKey(addr) && trackedReasons.containsKey(addr)) {
                Label subLabel = subtitleLabelMap.get(addr);
                if (subLabel != null) {
                    subLabel.setText(trackedReasons.get(addr));
                }
            }

            styleRow(addr, changedAddresses.contains(addr));
        }
    }

    private VBox buildRow(long addr, String labelName, String reason) {
        boolean hasLabel = labelName != null;

        Label nameLabel = new Label(hasLabel ? labelName : String.format("0x%X", addr));
        nameLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AMBER + ";"
        );

        String subtitle;
        if (hasLabel) {
            subtitle = String.format("0x%X", addr);
        } else if (reason != null) {
            subtitle = reason;
        } else {
            subtitle = "Tracked Address";
        }

        Label addrLabel = new Label(subtitle);
        addrLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 10.5;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";"
        );
        subtitleLabelMap.put(addr, addrLabel);

        VBox leftCol = new VBox(2, nameLabel, addrLabel);

        Label valLabel = new Label("0x0");
        valLabel.setStyle(
                "-fx-font-family: " + MONO + ";" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        valueLabelMap.put(addr, valLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, leftCol, spacer, valLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.setPadding(new Insets(8, 10, 8, 10));
        styleRow(card, false);

        card.setOnMouseEntered(e -> {
            if (!changedAddresses.contains(addr)) {
                card.setStyle(rowStyle(true, false));
            }
        });
        card.setOnMouseExited(e -> styleRow(addr, changedAddresses.contains(addr)));

        return card;
    }

    private void styleRow(long addr, boolean changed) {
        VBox card = rowNodes.get(addr);
        if (card != null) {
            styleRow(card, changed);
        }
    }

    private void styleRow(VBox card, boolean changed) {
        card.setStyle(rowStyle(false, changed));
    }

    private String rowStyle(boolean hover, boolean changed) {
        String bg = changed ? CHANGED_BG : (hover ? BG_HOVER : BG_RAISED);
        String border = changed ? CHANGED_BDR : BORDER_SOFT;
        return "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;";
    }

    private String formatHex(long v) {
        if (v == 0) return "0x0";
        return "0x" + Long.toHexString(v).toUpperCase();
    }
}