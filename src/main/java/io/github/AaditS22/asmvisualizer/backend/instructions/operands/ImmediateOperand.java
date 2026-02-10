package io.github.AaditS22.asmvisualizer.backend.instructions.operands;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

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
            } else {
                throw new IllegalArgumentException("Immediate value is not a number or label: " + rawText);
            }
        }
    }

    @Override
    public void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize) {
        throw new UnsupportedOperationException("Cannot set value of immediate operand!");
    }
}
