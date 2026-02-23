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
    private static final Set<String> SPECIAL_FUNCTIONS = Set.of("printf", "scanf", "exit");

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
            if (name.equals("scanf")) {
                long returnAddress = state.getPC() + MemoryLayout.INSTRUCTION_SIZE;
                state.getMemory().stackPush(returnAddress);
                ScanfHandler.execute(state);
                if (state.getIOBuffer().isWaitingForInput()) {
                    state.getMemory().stackPop();
                    return;
                }
                state.getMemory().stackPop();
                state.nextInstruction();
                return;
            }
            if (name.equals("exit")) {
                long code = state.getRegister("rdi", 8);
                state.getIOBuffer().requestExit(code);
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
                return "Called the special function 'printf'. Used the format string in %rdi and values from "
                        + "%rsi, %rdx, %rcx, %r8, and %r9 (and the stack if needed) to print formatted output." +
                        " Set %rax to the number of characters printed.";
            }
            if (name.equals("scanf")) {
                if (!state.getIOBuffer().hasInput()) {
                    return "Called the special function 'scanf'. Uses the format string in %rdi and pointer arguments "
                            + "in %rsi, %rdx, %rcx, %r8, and %r9. Execution pauses until the user provides input.";
                } else {
                    return "Resumed 'scanf' with user input. Parsed values were written to the memory addresses "
                            + "given by the pointer arguments. Set %rax to the number of values successfully read.";
                }
            }
            if (name.equals("exit")) {
                return "Called the special function 'exit'. Terminated the program with the exit code "
                        + " from %rdi (0 = success, non-zero = error).";
            }
        }

        return "Pushed the return address (address of next instruction) " +
                "onto the stack, then jumped to the instruction at: "
                + target.getDescription(state, labelManager);
    }
}
