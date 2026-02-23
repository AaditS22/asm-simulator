package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class AddInstruction extends Instruction {
    public AddInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        if (operands.get(0) instanceof MemoryOperand && operands.get(1) instanceof MemoryOperand) {
            throw new IllegalArgumentException("ADD cannot have two memory operands");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long op1 = operands.get(0).getValue(state, labelManager, size);
        long op2 = operands.get(1).getValue(state, labelManager, size);
        long value = op1 + op2;
        state.getFlags().updateAddFlags(op1, op2, size.getBytes() * 8);
        operands.get(1).setValue(state, labelManager, value, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Added " + operands.get(0).getDescription(state, labelManager) + " and " +
                operands.get(1).getDescription(state, labelManager) + " and stored the result in the" +
                " latter operand";
    }
}
