package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;
import java.util.Set;

public class CallInstruction extends Instruction {
    private static final Set<String> SPECIAL_FUNCTIONS = Set.of("printf");

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
        if (target instanceof LabelOperand labelOperand) {
            String name = labelOperand.toAssemblyString();
            if (name.equals("printf")) {
                long returnAddress = state.getPC() + MemoryLayout.INSTRUCTION_SIZE;
                state.getMemory().stackPush(returnAddress);

                PrintfHandler.execute(state);

                state.getMemory().stackPop();
                state.nextInstruction();
                return;
            }
            if (SPECIAL_FUNCTIONS.contains(name) && !labelManager.isCodeLabel(name)) {
                throw new IllegalArgumentException(
                        "Call to '" + name + "' is a special function but not yet supported");
            }
        }
        long returnAddress = state.getPC() + MemoryLayout.INSTRUCTION_SIZE;
        state.getMemory().stackPush(returnAddress);
        long jumpAddress = target.getValue(state, labelManager, Size.QUAD);
        state.setPC(jumpAddress);
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        Operand target = operands.get(0);

        if (target instanceof LabelOperand labelOperand) {
            String name = labelOperand.toAssemblyString();
            if (name.equals("printf")) {
                return "Calls the special function 'printf'. Gets the format string from %rdi and " +
                        "collects arguments from %rsi, %rdx, %rcx, %r8, and %r9 (and more from the stack if needed)" +
                        " to print to the standard output. The return address of the next instruction is pushed to the"
                        + " stack and popped immediately after execution.";
            }
        }

        return "Pushes the return address (address of next instruction) " +
                "onto the stack, then jumps to the instruction with address: "
                + target.getDescription(state, labelManager);
    }
}
