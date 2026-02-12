package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.Size;

public interface Operand {
    /**
     * Retrieves the value associated with this operand based on the current CPU state and label manager.
     *
     * @param state the current state of the CPU, including registers, memory, and flags
     * @param labelManager the label manager that contains information about code and data labels
     * @param operationSize the size of the operation, as a Size enum
     * @return the value determined by this operand
     */
    long getValue(CPUState state, LabelManager labelManager, Size operationSize);

    /**
     * Updates the value associated with this operand
     *
     * @param state the current state of the CPU, which includes registers, memory, and flags
     * @param labelManager the label manager containing information about code and data labels
     * @param value the new value to set for this operand
     * @param operationSize the size of the operation, as a Size enum
     */
    void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize);

    /**
     * Returns a detailed, human-readable description of this operand.
     *
     * @param state the current state of the CPU, including registers, memory, and flags
     * @param labelManager the label manager containing information about code and data labels
     * @return a human-readable description of this operand
     */
    String getDescription(CPUState state, LabelManager labelManager);

    /**
     * Returns the assembly syntax representation.
     * @return the assembly syntax representation of this operand
     */
    String toAssemblyString();
}
