package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.Size;

public class ImmediateOperand implements Operand {
    private final String rawText;

    public ImmediateOperand(String rawText) {
        this.rawText = rawText.trim().replaceAll("\\$", "");
    }

    @Override
    public long getValue(CPUState state, LabelManager labelManager, Size operationSize) {
        try {
            return Long.decode(rawText);
        } catch (NumberFormatException e) {
            // Check if the label exists
            if (labelManager.isDataLabel(rawText)) {
                return labelManager.getDataLabel(rawText).address();
            } else if (labelManager.isCodeLabel(rawText)) {
                return labelManager.getCodeLabel(rawText);
            } else {
                throw new IllegalArgumentException("Immediate value is not a number or label: " + rawText);
            }
        }
    }

    @Override
    public void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize) {
        throw new UnsupportedOperationException("Cannot set value of immediate operand!");
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        long value = getValue(state, labelManager, null);
        return String.format("immediate value $%s (%d, 0x%X)", rawText, value, value);
    }

    @Override
    public String toAssemblyString() {
        return String.format("$%s", rawText);
    }
}
