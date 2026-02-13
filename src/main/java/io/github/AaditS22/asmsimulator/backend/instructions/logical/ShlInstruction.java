package io.github.AaditS22.asmsimulator.backend.instructions.logical;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class ShlInstruction extends Instruction {
    public ShlInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        int numBits = size.getBytes() * 8;
        long count = operands.get(0).getValue(state, labelManager, size);
        long value = operands.get(1).getValue(state, labelManager, size);

        int countMask = (numBits == 64) ? 0x3F : 0x1F;
        count &= countMask;

        long result = value << count;
        state.getFlags().updateShlFlags(value, count, numBits);
        operands.get(1).setValue(state, labelManager, result, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Shifts " + operands.get(1).getDescription(state, labelManager)
                + " left by " + operands.get(0).getDescription(state, labelManager) + " bits";
    }
}
