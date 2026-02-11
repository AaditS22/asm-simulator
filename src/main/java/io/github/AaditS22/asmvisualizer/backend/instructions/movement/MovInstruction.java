package io.github.AaditS22.asmvisualizer.backend.instructions.movement;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.Instruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.Operand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.util.List;

public class MovInstruction extends Instruction {
    public MovInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        Operand src = operands.get(0);
        Operand dest = operands.get(1);
        dest.setValue(state, labelManager, src.getValue(state, labelManager, size), size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        Operand dest = operands.get(1);
        if (dest instanceof MemoryOperand) {
            return "Moves " + operands.get(0).getDescription(state, labelManager) + " to "
                    + dest.getDescription(state, labelManager)
                    .replace("memory at ", "");
        }
        return "Moves " + operands.get(0).getDescription(state, labelManager) + " to "
                + dest.getDescription(state, labelManager);
    }
}
