package io.github.AaditS22.asmvisualizer.backend.instructions.movement;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.Instruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.Operand;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.util.List;

public class PopInstruction extends Instruction {
    public PopInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long value = state.getMemory().stackPop();
        operands.get(0).setValue(state, labelManager, value, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        Operand dest = operands.get(0);
        String destString = dest.getDescription(state, labelManager);
        if (dest instanceof MemoryOperand) {
            destString = destString.replace("memory at ", "");
        }
        return "Pops the top of the stack into " + destString;
    }
}
