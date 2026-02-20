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
                return "Calls the special function 'printf'. Gets the format string from %rdi and " +
                        "collects arguments from %rsi, %rdx, %rcx, %r8, and %r9 (and more from the stack if needed)" +
                        " to print to the standard output. The return address of the next instruction is pushed to the"
                        + " stack and popped immediately after execution.";
            }
            if (name.equals("scanf")) {
                if (!state.getIOBuffer().hasInput()) {
                    return "Calls the special function 'scanf'. Reads the format string from %rdi " +
                            "and expects pointer arguments in %rsi, %rdx, %rcx, "
                            + "%r8, and %r9 (and more from the stack if needed). Each pointer is a "
                            + "memory address where a parsed input value will be written. Execution is "
                            + "now paused until the user provides input.";
                } else {
                    return "Calls the special function 'scanf'. Resumes with the provided user input, "
                            + "parsing it against the format string . Each matched "
                            + "value is written to the memory address held in the corresponding pointer "
                            + "argument. Sets %rax to the number of items successfully matched and stored. "
                            + "The return address of the next instruction is pushed to the stack and "
                            + "popped immediately after execution.";
                }
            }
            if (name.equals("exit")) {
                long code = state.getRegister("rdi", 8);
                return "Calls the special function 'exit'. Reads the exit status code from %rdi "
                        + "(currently " + code + ") and immediately terminates the program. "
                        + "By convention, a status of 0 means success and any non-zero value "
                        + "indicates an error or abnormal termination.";
            }
        }

        return "Pushes the return address (address of next instruction) " +
                "onto the stack, then jumps to the instruction with address: "
                + target.getDescription(state, labelManager);
    }
}
