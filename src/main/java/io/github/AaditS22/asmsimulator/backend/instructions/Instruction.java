package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovzbInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovzwInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public abstract class Instruction {
    protected final List<Operand> operands;
    protected final String mnemonic;
    protected final Size size;

    public Instruction(String mnemonic, Size size, List<Operand> operands) {
        this.mnemonic = mnemonic;
        this.size = size;
        this.operands = operands;
        if (operands.size() == 2) {
            if (operands.get(0) instanceof MemoryOperand && operands.get(1) instanceof MemoryOperand) {
                throw new IllegalArgumentException("Instructions cannot have two memory operands");
            }
        }
        if (!(this instanceof MovzbInstruction) && !(this instanceof MovzwInstruction)) {
            for (Operand operand : operands) {
                if (operand instanceof RegisterOperand registerOperand) {
                    registerOperand.validateSize(size);
                }
            }
        }
    }

    /**
     * Executes the current instruction.
     *
     * @param state the current state of the CPU, including registers, memory, and flags
     * @param labelManager the label management system containing code and data labels
     */
    public abstract void execute(CPUState state, LabelManager labelManager);

    /**
     * Returns a description of the current instruction.
     * @param state the current state of the CPU, including registers, memory, and flags
     * @param labelManager the label management system containing code and data labels
     * @return a String describing the current instruction
     */
    public abstract String getDescription(CPUState state, LabelManager labelManager);

    // Getters for all values

    public String getMnemonic() {
        return mnemonic;
    }

    public Size getSize() {
        return size;
    }

    public List<Operand> getOperands() {
        return operands;
    }

    /**
     * Validates whether the number of operands in the current instruction matches the expected count.
     *
     * @param expectedCount the number of operands expected for the instruction
     * @throws IllegalArgumentException if the actual operand count does not match the expected count
     */
    public void validateOperandCount(int expectedCount) {
        if (operands.size() != expectedCount) {
            throw new IllegalArgumentException(
                    mnemonic + " expects " + expectedCount + " operand(s), got " + operands.size()
            );
        }
    }
}
