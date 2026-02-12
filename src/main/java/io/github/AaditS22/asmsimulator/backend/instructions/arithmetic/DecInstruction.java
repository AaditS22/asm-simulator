package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class DecInstruction extends Instruction {
    public DecInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("DEC cannot be used with an immediate");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long value = operands.get(0).getValue(state, labelManager, size) - 1;
        operands.get(0).setValue(state, labelManager, value, size);
        state.getFlags().updateDecFlags(value + 1, size.getBytes() * 8);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Decrements " + operands.get(0).getDescription(state, labelManager) + " by 1";
    }
}
