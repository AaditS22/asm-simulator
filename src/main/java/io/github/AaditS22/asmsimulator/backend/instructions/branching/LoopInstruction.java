package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class LoopInstruction extends Instruction {
    public LoopInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (!(operands.get(0) instanceof LabelOperand)) {
            throw new IllegalArgumentException("LOOP requires a label operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long newRcx = state.getRegister("rcx", 8) - 1;
        state.setRegister("rcx", 8, newRcx);
        if (newRcx != 0) {
            long address = operands.get(0).getValue(state, labelManager, null);
            state.setPC(address);
        } else {
            state.nextInstruction();
        }
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Decrements %%rcx by 1 and jumps to " + operands.get(0).getDescription(state, labelManager)
                + " if the new value is not zero";
    }
}
