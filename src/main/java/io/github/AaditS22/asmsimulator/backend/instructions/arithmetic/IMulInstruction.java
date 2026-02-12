package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.math.BigInteger;
import java.util.List;

public class IMulInstruction extends Instruction {
    public IMulInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);

        int count = operands.size();

        if (count == 1) {
            if (operands.get(0) instanceof ImmediateOperand) {
                throw new IllegalArgumentException("IMUL one-operand form cannot use an immediate");
            }
        } else if (count == 2) {
            if (size == Size.BYTE) {
                throw new IllegalArgumentException("IMUL two-operand form does not support byte size");
            }
            if (!(operands.get(1) instanceof RegisterOperand)) {
                throw new IllegalArgumentException("IMUL two-operand form: destination must be a register");
            }
            if (operands.get(0) instanceof MemoryOperand && operands.get(1) instanceof MemoryOperand) {
                throw new IllegalArgumentException("IMUL cannot have two memory operands");
            }
        } else {
            if (size == Size.BYTE) {
                throw new IllegalArgumentException("IMUL three-operand form does not support byte size");
            }
            if (!(operands.get(0) instanceof ImmediateOperand)) {
                throw new IllegalArgumentException("IMUL three-operand form: first operand must be an immediate");
            }
            if (!(operands.get(2) instanceof RegisterOperand)) {
                throw new IllegalArgumentException("IMUL three-operand form: destination must be a register");
            }
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        int numBits = size.getBytes() * 8;

        switch (operands.size()) {
            case 1 -> executeOneOperand(state, labelManager, numBits);
            case 2 -> executeTwoOperand(state, labelManager, numBits);
            case 3 -> executeThreeOperand(state, labelManager, numBits);
            default -> throw new IllegalStateException("Unexpected operand count");
        }

        state.nextInstruction();
    }

    /**
     * One-operand form for IMUL (same as MUL but signed)
     *
     * @param state the CPU state containing registers, memory, and flags
     * @param labelManager the label manager to resolve any labels used in the instruction
     * @param numBits the number of bits used for sign extension and masking
     */
    private void executeOneOperand(CPUState state, LabelManager labelManager, int numBits) {
        long src = operands.get(0).getValue(state, labelManager, size);

        if (size == Size.BYTE) {
            long al = signExtend(state.getRegister("rax", 1), 8);
            long srcSigned = signExtend(src, 8);
            long result = al * srcSigned;
            state.setRegister("rax", 2, result & 0xFFFF);
        } else if (size == Size.QUAD) {
            BigInteger a = BigInteger.valueOf(state.getRegister("rax", 8));
            BigInteger b = BigInteger.valueOf(src);
            BigInteger result = a.multiply(b);
            long low = result.longValue();
            long high = result.shiftRight(64).longValue();
            state.setRegister("rax", 8, low);
            state.setRegister("rdx", 8, high);

        } else {
            long acc = signExtend(state.getRegister("rax", size.getBytes()), numBits);
            long srcSigned = signExtend(src, numBits);
            long result = acc * srcSigned;
            long mask = (1L << numBits) - 1;
            long low = result & mask;
            long high = (result >> numBits) & mask;

            state.setRegister("rax", size.getBytes(), low);
            state.setRegister("rdx", size.getBytes(), high);
        }
    }

    /**
     * Two-operand form for IMul (takes a src and dst, does dst = dst * src)
     *
     * @param state the current CPU state, including registers, memory, and flags
     * @param labelManager the manager used for resolving labels in the instruction
     * @param numBits the bit width used for sign extension and truncation of the result
     */
    private void executeTwoOperand(CPUState state, LabelManager labelManager, int numBits) {
        long src = signExtend(operands.get(0).getValue(state, labelManager, size), numBits);
        long dst = signExtend(operands.get(1).getValue(state, labelManager, size), numBits);

        long fullResult = src * dst;
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        long truncated = fullResult & mask;

        operands.get(1).setValue(state, labelManager, truncated, size);
    }

    /**
     * Three-operand form for IMul (take an immediate, src, and dst, does dst = src * imm)
     *
     * @param state the current CPU state, including registers, memory, and flags
     * @param labelManager the manager used to resolve any labels in the instruction
     * @param numBits the number of bits used for sign extension, multiplications, and truncation
     */
    private void executeThreeOperand(CPUState state, LabelManager labelManager, int numBits) {
        long imm = signExtend(operands.get(0).getValue(state, labelManager, size), numBits);
        long src = signExtend(operands.get(1).getValue(state, labelManager, size), numBits);

        long fullResult = imm * src;
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        long truncated = fullResult & mask;

        operands.get(2).setValue(state, labelManager, truncated, size);
    }

    /**
     * Performs sign extension on a value by extending its sign bit
     * DISCLAIMER: This method was written by AI
     *
     * @param value the value to be sign-extended
     * @param bits the number of bits in the original value
     * @return the sign-extended value
     */
    private static long signExtend(long value, int bits) {
        long shift = 64 - bits;
        return (value << shift) >> shift;
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        if (operands.size() == 1) {
            String srcDesc = operands.get(0).getDescription(state, labelManager);
            return switch (size) {
                case BYTE -> "Multiplies (signed) AL with " + srcDesc +
                        " and stores the 16-bit result in AX";

                case WORD -> "Multiplies (signed) AX with " + srcDesc +
                        " and stores the 32-bit result across DX (high 16 bits) and AX (low 16 bits)";

                case LONG -> "Multiplies (signed) EAX with " + srcDesc +
                        " and stores the 64-bit result across EDX (high 32 bits) and EAX (low 32 bits)";

                case QUAD -> "Multiplies (signed) RAX with " + srcDesc +
                        " and stores the 128-bit result across RDX (high 32 bits) and RAX (low 32 bits)";
            };
        }

        else if (operands.size() == 2) {
            return "Multiplies (signed) "
                    + operands.get(1).getDescription(state, labelManager)
                    + " with "
                    + operands.get(0).getDescription(state, labelManager)
                    + " and stores the truncated "
                    + (size.getBytes() * 8)
                    + "-bit result back into the destination register";
        }

        else {
            return "Multiplies (signed) "
                    + operands.get(1).getDescription(state, labelManager)
                    + " with "
                    + operands.get(0).getDescription(state, labelManager)
                    + " and stores the truncated "
                    + (size.getBytes() * 8)
                    + "-bit result into "
                    + operands.get(2).getDescription(state, labelManager);
        }
    }
}
