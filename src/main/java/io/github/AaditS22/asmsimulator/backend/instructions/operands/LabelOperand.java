package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.Size;

public class LabelOperand implements Operand {
    private final String labelName;

    public LabelOperand(String labelName) {
        this.labelName = labelName.trim();
    }

    @Override
    public long getValue(CPUState state, LabelManager labelManager, Size operationSize) {
        if (labelManager.isCodeLabel(labelName)) {
            return labelManager.getCodeLabel(labelName);
        } else if (labelManager.isDataLabel(labelName)) {
            return labelManager.getDataLabel(labelName).address();
        } else {
            throw new IllegalArgumentException("Undefined label: " + labelName);
        }
    }

    @Override
    public void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize) {
        throw new UnsupportedOperationException("Cannot set value of a label operand!");
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        long address = getValue(state, labelManager, null);
        return String.format("label '%s' (address 0x%X)", labelName, address);
    }

    @Override
    public String toAssemblyString() {
        return labelName;
    }
}