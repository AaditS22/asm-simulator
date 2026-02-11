package io.github.AaditS22.asmvisualizer.backend.instructions.movement;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.Instruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.Operand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.util.List;

public class LeaInstruction extends Instruction {
    public LeaInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(2);

        // Validate first operand is memory
        if (!(operands.get(0) instanceof MemoryOperand)) {
            throw new IllegalArgumentException("LEA requires a memory operand as source");
        }

        // Validate second operand is register
        if (!(operands.get(1) instanceof RegisterOperand)) {
            throw new IllegalArgumentException("LEA requires a register operand as destination");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        MemoryOperand operand = (MemoryOperand) operands.get(0);
        long address = operand.calculateAddress(state, labelManager);
        operands.get(1).setValue(state, labelManager, address, size);
        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        String memoryDesc = operands.get(0).getDescription(state, labelManager);
        String updated = memoryDesc.replace("memory at ", "");
        return "Loads " + updated + " into " + operands.get(1).getDescription(state, labelManager);
    }
}
