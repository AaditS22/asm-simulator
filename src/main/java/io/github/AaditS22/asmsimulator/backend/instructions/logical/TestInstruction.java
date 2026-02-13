package io.github.AaditS22.asmsimulator.backend.instructions.logical;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class TestInstruction extends Instruction {
    public TestInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        if (operands.get(0) instanceof ImmediateOperand && operands.get(1) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("TEST cannot be used with two immediate operands");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long op1 = operands.get(0).getValue(state, labelManager, size);
        long op2 = operands.get(1).getValue(state, labelManager, size);
        long result = op1 & op2;
        state.getFlags().updateLogicalFlags(result, size.getBytes() * 8);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Bitwise AND of " + operands.get(0).getDescription(state, labelManager)
                + " and " + operands.get(1).getDescription(state, labelManager)
                + ", updating flags based on the result";
    }
}
