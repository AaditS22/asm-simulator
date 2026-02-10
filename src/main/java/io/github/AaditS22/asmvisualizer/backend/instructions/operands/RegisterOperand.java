package io.github.AaditS22.asmvisualizer.backend.instructions.operands;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.util.RegisterInfo;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

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
}
