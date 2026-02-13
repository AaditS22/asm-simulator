package io.github.AaditS22.asmsimulator.backend.instructions.logical;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class NegInstruction extends Instruction {
    public NegInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("NEG cannot be used with an immediate operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long value = operands.get(0).getValue(state, labelManager, size);
        long result = -value;
        state.getFlags().updateNegateFlags(value, size.getBytes() * 8);
        operands.get(0).setValue(state, labelManager, result, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Two's complement negation of " + operands.get(0).getDescription(state, labelManager);
    }
}
