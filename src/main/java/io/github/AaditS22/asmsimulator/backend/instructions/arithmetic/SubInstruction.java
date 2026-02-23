package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class SubInstruction extends Instruction {
    public SubInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        if (operands.get(0) instanceof MemoryOperand && operands.get(1) instanceof MemoryOperand) {
            throw new IllegalArgumentException("SUB cannot have two memory operands");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long op1 = operands.get(0).getValue(state, labelManager, size);
        long op2 = operands.get(1).getValue(state, labelManager, size);
        long value = op2 - op1;
        state.getFlags().updateSubFlags(op2, op1, size.getBytes() * 8);
        operands.get(1).setValue(state, labelManager, value, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Subtracted " + operands.get(0).getDescription(state, labelManager)
                + " from " + operands.get(1).getDescription(state, labelManager)
                + " and stored the result in the latter operand";
    }
}
