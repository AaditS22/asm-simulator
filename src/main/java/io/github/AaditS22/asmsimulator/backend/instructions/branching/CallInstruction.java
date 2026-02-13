package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class CallInstruction extends Instruction {
    public CallInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("CALL cannot be used with an immediate operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        Operand target = operands.get(0);
        long returnAddress = state.getPC() + MemoryLayout.INSTRUCTION_SIZE;
        state.getMemory().stackPush(returnAddress);
        long jumpAddress = target.getValue(state, labelManager, Size.QUAD);
        state.setPC(jumpAddress);
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        return "Pushes the return address (address of next instruction) " +
                "onto the stack, then jumps to the instruction with address: "
                + operands.get(0).getDescription(state, labelManager);
    }
}
