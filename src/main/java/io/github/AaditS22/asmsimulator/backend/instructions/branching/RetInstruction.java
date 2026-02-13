package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class RetInstruction extends Instruction {
    public RetInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(0);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long address = state.getMemory().stackPop();
        state.setPC(address);
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Pops the return address from the top of the stack and jumps to it";
    }
}
