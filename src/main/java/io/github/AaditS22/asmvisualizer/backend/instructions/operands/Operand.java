package io.github.AaditS22.asmvisualizer.backend.instructions.operands;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;

public interface Operand {
    /**
     * Retrieves the value associated with this operand based on the current CPU state and label manager.
     *
     * @param state the current state of the CPU, including registers, memory, and flags
     * @param labelManager the label manager that contains information about code and data labels
     * @return the value determined by this operand
     */
    long getValue(CPUState state, LabelManager labelManager);

    /**
     * Updates the value associated with this operand
     *
     * @param state the current state of the CPU, which includes registers, memory, and flags
     * @param labelManager the label manager containing information about code and data labels
     * @param value the new value to set for this operand
     */
    void setValue(CPUState state, LabelManager labelManager, long value);
}
