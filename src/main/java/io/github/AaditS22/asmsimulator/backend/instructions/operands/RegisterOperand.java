package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.RegisterInfo;
import io.github.AaditS22.asmsimulator.backend.util.Size;

public class RegisterOperand implements Operand {
    private final RegisterInfo register;

    public RegisterOperand(String registerName) {
        if (!registerName.contains("%")) throw new IllegalArgumentException("Invalid register name: " + registerName);
        if (registerName.trim().equalsIgnoreCase("%rip")) throw new
                IllegalArgumentException("Cannot use %rip as a register operand!");
        try {
            this.register = RegisterInfo.valueOf(registerName.trim().replace("%", "").toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid register name: " + registerName);
        }
    }

    @Override
    public long getValue(CPUState state, LabelManager labelManager, Size operationSize) {
        // Check if there is a size mismatch between register size and operation
        if (operationSize != null && register.getNumBytes() != operationSize.getBytes()) {
            throw new IllegalArgumentException("The register: " + register.name() +
                    " is not the correct size for this operation!");
        }
        return state.getRegister(register.getBaseRegister(), register.getNumBytes());
    }

    @Override
    public void setValue(CPUState state, LabelManager labelManager, long value, Size operationSize) {
        // Check if there is a size mismatch between register size and operation
        if (register.getNumBytes() != operationSize.getBytes()) {
            throw new IllegalArgumentException("The register: " + register.name() +
                    " is not the correct size for this operation!");
        }
        state.setRegister(register.getBaseRegister(), register.getNumBytes(), value);
    }

    /**
     * Gets the size of this register operand
     * @return the Size enum corresponding to this register's byte width
     */
    public Size getSize() {
        return switch (register.getNumBytes()) {
            case 1 -> Size.BYTE;
            case 2 -> Size.WORD;
            case 4 -> Size.LONG;
            case 8 -> Size.QUAD;
            default -> throw new IllegalStateException("Unknown register size: " + register.getNumBytes());
        };
    }

    /**
     * Helper method to validate the size of a register operand
     * @param instructionSize the size of the instruction
     */
    public void validateSize(Size instructionSize) {
        if (instructionSize != null && register.getNumBytes() != instructionSize.getBytes()) {
            throw new IllegalArgumentException("%" + register.name().toLowerCase()
                    + " is not the correct size for a " + instructionSize + " operation");
        }
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        long value = state.getRegister(register.getBaseRegister(), register.getNumBytes());
        return String.format("register %%%s [current value: %d (0x%X)]",
                register.name().toLowerCase(), value, value);
    }

    @Override
    public String toAssemblyString() {
        return "%" + register.name().toLowerCase();
    }
}
