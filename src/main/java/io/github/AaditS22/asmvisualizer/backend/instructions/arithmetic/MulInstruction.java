package io.github.AaditS22.asmvisualizer.backend.instructions.arithmetic;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.Instruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.Operand;
import io.github.AaditS22.asmvisualizer.backend.util.Size;

import java.math.BigInteger;
import java.util.List;

public class MulInstruction extends Instruction {
    public MulInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("MUL cannot be used with an immediate operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        int numBits = size.getBytes() * 8;
        long src = operands.get(0).getValue(state, labelManager, size);

        if (size == Size.BYTE) {
            long al = state.getRegister("rax", 1);
            long result = (al & 0xFF) * (src & 0xFF);
            state.setRegister("rax", 2, result & 0xFFFF);

        } else if (size == Size.QUAD) {
            // BigInteger required because 64-bit multiplication produces a 128-bit result
            BigInteger a = toUnsignedBigInt(state.getRegister("rax", 8));
            BigInteger b = toUnsignedBigInt(src);
            BigInteger result = a.multiply(b);
            // Separate lower and upper half, store in rax and rdx following the x86 standard
            long low = result.longValue();
            long high = result.shiftRight(64).longValue();
            state.setRegister("rax", 8, low);
            state.setRegister("rdx", 8, high);

        } else {
            long mask = (1L << numBits) - 1;
            long raxVal = state.getRegister("rax", size.getBytes()) & mask;
            long result = raxVal * (src & mask);
            long low = result & mask;
            long high = (result >> numBits) & mask;

            state.setRegister("rax", size.getBytes(), low);
            state.setRegister("rdx", size.getBytes(), high);
        }

        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        String srcDesc = operands.get(0).getDescription(state, labelManager);
        return switch (size) {
            case BYTE -> "Multiplies (unsigned) AL with " + srcDesc +
                    " and stores the 16-bit result in AX";
            case WORD -> "Multiplies (unsigned) AX with " + srcDesc +
                    " and stores the 32-bit result across DX (high 16 bits) and AX (low 16 bits)";

            case LONG -> "Multiplies (unsigned) EAX with " + srcDesc +
                    " and stores the 64-bit result across EDX (high 32 bits) and EAX (low 32 bits)";

            case QUAD -> "Multiplies (unsigned) RAX with " + srcDesc +
                    " and stores the 128-bit result across RDX (high 64 bits) and RAX (low 64 bits)";

        };
    }

    /**
     * Converts a long to an unsigned BigInteger for 64-bit multiplication
     */
    private static BigInteger toUnsignedBigInt(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }
}
