package io.github.AaditS22.asmsimulator.backend.instructions.movement;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class MovzbInstruction extends Instruction {
    public MovzbInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        // Validate second operand
        if (!(operands.get(1) instanceof RegisterOperand)) {
            throw new IllegalArgumentException("MOVZB requires a register operand as destination");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long byteValue = operands.get(0).getValue(state, labelManager, Size.BYTE) & 0xFFL;
        Size destSize = ((RegisterOperand) operands.get(1)).getSize();
        operands.get(1).setValue(state, labelManager, byteValue, destSize);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Copied the low byte of " + operands.get(0).getDescription(state, labelManager) + " to "
                + operands.get(1).getDescription(state, labelManager) + " (zero-extended)";
    }
}
