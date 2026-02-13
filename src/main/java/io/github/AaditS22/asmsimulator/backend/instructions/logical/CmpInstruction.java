package io.github.AaditS22.asmsimulator.backend.instructions.logical;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class CmpInstruction extends Instruction {
    public CmpInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        if (operands.get(0) instanceof ImmediateOperand && operands.get(1) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("CMP cannot be used with two immediate operands");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long op1 = operands.get(0).getValue(state, labelManager, size);
        long op2 = operands.get(1).getValue(state, labelManager, size);
        state.getFlags().updateSubFlags(op2, op1, size.getBytes() * 8);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Subtracts " + operands.get(0).getDescription(state, labelManager)
                + " from " + operands.get(1).getDescription(state, labelManager)
                + " and updates flags based on the result";
    }
}
