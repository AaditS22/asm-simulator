package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.math.BigInteger;
import java.util.List;

public class DivInstruction extends Instruction {
    public DivInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("DIV cannot be used with an immediate operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long divisor = operands.get(0).getValue(state, labelManager, size);
        int numBits = size.getBytes() * 8;
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;

        if ((divisor & mask) == 0) {
            throw new ArithmeticException("Cannot divide by zero in DIV operation");
        }

        if (size == Size.BYTE) {
            long ax = state.getRegister("rax", 2) & 0xFFFF;
            long div = divisor & 0xFF;
            long quotient = Long.divideUnsigned(ax, div);
            long remainder = Long.remainderUnsigned(ax, div);

            if (quotient > 0xFF) {
                throw new ArithmeticException("DIV: quotient overflow (doesn't fit in AL)");
            }

            long result = ((remainder & 0xFF) << 8) | (quotient & 0xFF);
            state.setRegister("rax", 2, result);

        } else if (size == Size.QUAD) {
            BigInteger high = toUnsignedBigInt(state.getRegister("rdx", 8));
            BigInteger low = toUnsignedBigInt(state.getRegister("rax", 8));
            BigInteger dividend = high.shiftLeft(64).or(low);
            BigInteger div = toUnsignedBigInt(divisor);

            BigInteger quotient = dividend.divide(div);
            BigInteger remainder = dividend.remainder(div);

            if (quotient.bitLength() > 64) {
                throw new ArithmeticException("DIV: quotient overflow (doesn't fit in RAX)");
            }

            state.setRegister("rax", 8, quotient.longValue());
            state.setRegister("rdx", 8, remainder.longValue());

        } else {
            long highPart = state.getRegister("rdx", size.getBytes()) & mask;
            long lowPart = state.getRegister("rax", size.getBytes()) & mask;
            long dividend = (highPart << numBits) | lowPart;
            long div = divisor & mask;

            long quotient = Long.divideUnsigned(dividend, div);
            long remainder = Long.remainderUnsigned(dividend, div);

            if (Long.compareUnsigned(quotient, mask) > 0) {
                throw new ArithmeticException("DIV: quotient overflow (doesn't fit in destination)");
            }

            state.setRegister("rax", size.getBytes(), quotient & mask);
            state.setRegister("rdx", size.getBytes(), remainder & mask);
        }

        state.nextInstruction();
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        String srcDesc = operands.get(0).getDescription(state, labelManager);
        return switch (size) {
            case BYTE -> "Divides (unsigned) value in AX by " + srcDesc +
                    ", storing the 8-bit quotient in AL and the 8-bit remainder in AH";

            case WORD -> "Divides (unsigned) the value formed by DX (high 16 bits) and AX (low 16 bits) by " + srcDesc +
                    ", storing the 16-bit quotient in AX and the 16-bit remainder in DX";

            case LONG -> "Divides (unsigned) the value formed by EDX (high 32 bits) and EAX (low 32 bits) by "
                    + srcDesc + ", storing the 32-bit quotient in EAX and the 32-bit remainder in EDX";

            case QUAD -> "Divides (unsigned) the value formed by RDX (high 64 bits) and RAX (low 64 bits) by "
                    + srcDesc + ", storing the 64-bit quotient in RAX and the 64-bit remainder in RDX";
        };
    }

    /**
     * Converts a long to an unsigned BigInteger for 64-bit unsigned division.
     */
    private static BigInteger toUnsignedBigInt(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }
}
