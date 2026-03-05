package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.IOBuffer;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import java.util.List;

public class SyscallInstruction extends Instruction {
    public SyscallInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(0);
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long sysCode = state.getRegister("rax", 8);
        switch ((int) sysCode) {
            case 0 -> read(state);
            case 1 -> write(state);
            case 60, 231 -> exit(state);
            default -> throw new UnsupportedOperationException("The syscall code " + sysCode + " is not (yet) " +
                    "supported by the simulator. Current supported codes are 0, 1, 60, and 231.");
        }
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        long sysCode = state.getRegister("rax", 8);
        return switch ((int) sysCode) {
            case 0 -> {
                if (state.getIOBuffer().isWaitingForInput()) {
                    yield "Ran syscall 0 (read). Waiting for user input on stdin (terminal input). " +
                            "Execution is paused until input is provided.";
                }
                yield "Ran syscall 0 (read). Used the value at %rsi to determine the memory " +
                        "address to write input to, and the value at %rdx to determine the number of bytes to read.";
            }
            case 1 -> "Ran syscall 1 (write). Used the value at %rdi to determine the starting address of the " +
                    "string to write to stdout (terminal), and the value at %rdx to determine " +
                    "the number of bytes to write.";
            case 60 -> "Ran syscall 60 (exit). Terminated the program with the exit code " +
                    "from %rdi (0 = success, non-zero = error).";
            case 231 -> "Ran syscall 231 (exit). Terminated the program with the exit code " +
                    "from %rdi (0 = success, non-zero = error).";
            default -> "Syscall code " + sysCode + " is unknown or not (yet) supported by the simulator.";
        };
    }

    /**
     * Completes the syscall read operation
     * @param state the CPUState object
     */
    private void read(CPUState state) {
        long src = state.getRegister("rdi", 8);
        long dest = state.getRegister("rsi", 8);
        long count = state.getRegister("rdx", 8);

        if (src != 0) {
            throw new UnsupportedOperationException("Currently, only reading from stdin (terminal input, rdi code 0)," +
                    " is supported");
        }

        IOBuffer ioBuffer = state.getIOBuffer();
        if (!ioBuffer.hasInput()) {
            ioBuffer.setWaitingForInput(true);
            return;
        }

        String input = ioBuffer.consumeInput();
        if (!input.endsWith("\n")) input += "\n";

        long numToRead = Math.min(count, input.length());
        for (int i = 0; i < numToRead; i++) {
            state.getMemory().writeByte(dest + i, (byte) input.charAt(i));
        }
        state.setRegister("rax", 8, numToRead);
        state.nextInstruction();
    }

    /**
     * Completes the syscall write operation
     * @param state the CPUState object
     */
    private void write(CPUState state) {
        long dest = state.getRegister("rdi", 8);
        long src = state.getRegister("rsi", 8);
        long count = state.getRegister("rdx", 8);

        if (dest != 1) {
            throw new UnsupportedOperationException("Currently, only writing to stdout (standard terminal output, " +
                    "rdi code 1), is supported.");
        }

        StringBuilder sb = new StringBuilder();
        for (long i = 0; i < count; i++) {
            sb.append((char) (state.getMemory().readByte(src + i) & 0xFF));
        }
        state.getIOBuffer().append(sb.toString());
        state.setRegister("rax", 8, count);
        state.nextInstruction();
    }

    /**
     * Completes the syscall exit operation
     * @param state the CPUState object
     */
    private void exit(CPUState state) {
        long code = state.getRegister("rdi", 8);
        state.getIOBuffer().requestExit(code);
    }
}
