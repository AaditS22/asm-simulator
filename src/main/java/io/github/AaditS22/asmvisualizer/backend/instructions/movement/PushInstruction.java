package io.github.AaditS22.asmvisualizer.backend.instructions.movement;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.Instruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.Operand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.util.List;

public class PushInstruction extends Instruction {
    public PushInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long value = operands.get(0).getValue(state, labelManager, size);
        state.getMemory().stackPush(value);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        Operand src = operands.get(0);
        if (src instanceof RegisterOperand) {
            return "Pushes the value of " + src.getDescription(state, labelManager) + " onto the stack";
        }
        return "Pushes " + src.getDescription(state, labelManager) + " onto the stack";
    }
}
