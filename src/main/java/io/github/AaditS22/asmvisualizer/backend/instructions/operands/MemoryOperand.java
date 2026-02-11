package io.github.AaditS22.asmvisualizer.backend.instructions.operands;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemoryOperand implements Operand {
    private final String rawText;
    private final boolean onlyDisplacement;
    private static final List<Integer> acceptedScales = new ArrayList<>(Arrays.asList(1, 2, 4, 8));

    public MemoryOperand(String rawText) {
        this.rawText = rawText.trim();
        onlyDisplacement = !rawText.contains("(") && !rawText.contains(")");

        if (rawText.contains("(") != rawText.contains(")")) {
            throw new IllegalArgumentException("Mismatched parentheses in memory operand: " + rawText);
        }
    }

    /**
     * Calculates the displacement value for a memory operand
     *
     * @param text the text of the operand, excluding the brackets.
     * @param labelManager the manager responsible for storing labels
     * @return the computed displacement value
     */
    public long calculateDisplacement(String text, LabelManager labelManager) {
        if (text.isBlank()) {
            return 0;
        }
        try {
            return Long.decode(text);
        } catch (NumberFormatException e) {
            if (labelManager.isDataLabel(text)) {
                return labelManager.getDataLabel(text).address();
            } else if (labelManager.isCodeLabel(text)) {
                return labelManager.getCodeLabel(text);
            } else {
                throw new IllegalArgumentException(
                        "Displacement '" + text + "' is not a valid number or label"
                );
            }
        }
    }

    /**
     * Helper method to calculate the address of the memory operand
     *
     * @param state the current CPU state
     * @param labelManager the label manager
     * @return the address of the memory operand
     */
    public long calculateAddress(CPUState state, LabelManager labelManager) {
        long address;
        if (onlyDisplacement) {
            address = calculateDisplacement(rawText, labelManager);
        } else {
            String displacementStr = rawText.substring(0, rawText.indexOf("("));
            long displacement = calculateDisplacement(displacementStr, labelManager);
            String[] parts = openParentheses();
            if (parts[0].trim().equalsIgnoreCase("%rip")) {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("%rip can only be used with no index or scale factor");
                }
                try {
                    // If this works, do %rip + number, otherwise use only the label's address as address
                    long val = Long.decode(displacementStr);
                    return val + state.getRegister("rip", 8);
                } catch (NumberFormatException e) {
                    return displacement;
                }
            }
            long base = parts[0].isBlank() ? 0 :
                    new RegisterOperand(parts[0].trim()).getValue(state, labelManager, null);

            long index = 0;
            int scale = 1;

            if (parts.length >= 2 && !parts[1].isBlank()) {
                if (parts[1].trim().equalsIgnoreCase("%rip")) {
                    throw new IllegalArgumentException("Index cannot be %rip");
                }
                index = new RegisterOperand(parts[1].trim())
                        .getValue(state, labelManager, null);
            }

            if (parts.length == 3) {
                try {
                    scale = Integer.decode(parts[2].trim());
                    if (!acceptedScales.contains(scale)) {
                        throw new IllegalArgumentException(
                                "Invalid scale factor " + scale + ". Must be 1, 2, 4, or 8"
                        );
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Scale '" + parts[2] + "' is not a valid number"
                    );
                }
            }

            address = base + displacement + (index * scale);
        }
        return address;
    }

    /**
     * Helper method to get the strings inside the parentheses of the operand
     *
     * @return an array of strings inside the parentheses
     */
    private String[] openParentheses() {
        String parensContent = rawText.substring(
                rawText.indexOf("(") + 1,
                rawText.indexOf(")")
        );
        String[] parts = parensContent.split(",", -1);

        if (parts.length > 3) {
            throw new IllegalArgumentException(
                    "Invalid memory operand format: " + rawText + ". " +
                            "The maximum number of arguments inside brackets is 3."
            );
        }
        return parts;
    }

    @Override
    public long getValue(CPUState state, LabelManager labelManager, Size operationSize) {
        long address = calculateAddress(state, labelManager);
        return state.getMemory().readN(address, operationSize.getBytes());
    }

    @Override
    public void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize) {
        long address = calculateAddress(state, labelManager);
        state.getMemory().writeN(address, value, operationSize.getBytes());
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        long address = calculateAddress(state, labelManager);
        return String.format("memory at effective address 0x%X (resolved as: %s)",
                address, getCalculationFormula(labelManager));
    }

    /**
     * Generates the calculation formula for a memory operand based on its raw text
     *
     * @param labelManager the label manager used to resolve labels
     * @return a string representing the calculation formula for the memory operand
     */
    private String getCalculationFormula(LabelManager labelManager) {
        if (onlyDisplacement) {
            return labelManager.hasLabel(rawText) ? "address of label " + rawText : "displacement " + rawText;
        }

        String dispStr = rawText.substring(0, rawText.indexOf("("));
        String[] parts = openParentheses();

        if (parts[0].trim().equalsIgnoreCase("%rip")) {
            return (labelManager.hasLabel(dispStr))
                    ? "RIP-relative label " + dispStr
                    : "%rip + " + (dispStr.isBlank() ? "0" : dispStr);
        }

        StringBuilder formula = new StringBuilder();

        if (!dispStr.isBlank()) {
            formula.append("displacement[").append(dispStr).append("]");
        } else {
            formula.append("displacement[0]");
        }
        formula.append(" + ");

        String base = parts[0].trim();
        if (!base.isBlank()) {
            formula.append("base[").append(base).append("]");
        } else {
            formula.append("base[0]");
        }

        if (parts.length >= 2 && !parts[1].isBlank()) {
            formula.append(" + (");
            formula.append("index[").append(parts[1].trim()).append("]");
            if (parts.length == 3 && !parts[2].isBlank()) {
                formula.append(" * ").append("scale[").append(parts[2].trim()).append("])");
            } else {
                formula.append(" * scale[1])");
            }
        }

        return formula.toString();
    }

    @Override
    public String toAssemblyString() {
        return rawText;
    }
}
