package io.github.AaditS22.asmsimulator.backend.instructions.arithmetic;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.math.BigInteger;
import java.util.List;

public class IDivInstruction extends Instruction {
    public IDivInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (operands.get(0) instanceof ImmediateOperand) {
            throw new IllegalArgumentException("IDIV cannot be used with an immediate operand");
        }
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        long divisor = operands.get(0).getValue(state, labelManager, size);
        int numBits = size.getBytes() * 8;
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;

        long signedDivisor = signExtend(divisor, numBits);
        if (signedDivisor == 0) {
            throw new ArithmeticException("IDIV: division by zero");
        }

        if (size == Size.BYTE) {
            long ax = signExtend(state.getRegister("rax", 2), 16);
            long quotient = ax / signedDivisor;
            long remainder = ax % signedDivisor;

            if (quotient < -128 || quotient > 127) {
                throw new ArithmeticException("IDIV: quotient overflow (doesn't fit in AL)");
            }

            long result = ((remainder & 0xFF) << 8) | (quotient & 0xFF);
            state.setRegister("rax", 2, result & 0xFFFF);

        } else if (size == Size.QUAD) {
            BigInteger high = BigInteger.valueOf(state.getRegister("rdx", 8));
            BigInteger low = toUnsignedBigInt(state.getRegister("rax", 8));
            BigInteger dividend = high.shiftLeft(64).or(low);
            BigInteger div = BigInteger.valueOf(signedDivisor);

            BigInteger quotient = dividend.divide(div);
            BigInteger remainder = dividend.remainder(div);

            if (quotient.bitLength() >= 64) {
                throw new ArithmeticException("IDIV: quotient overflow (doesn't fit in RAX)");
            }

            state.setRegister("rax", 8, quotient.longValue());
            state.setRegister("rdx", 8, remainder.longValue());

        } else {
            long highPart = state.getRegister("rdx", size.getBytes()) & mask;
            long lowPart = state.getRegister("rax", size.getBytes()) & mask;
            long dividend = signExtend((highPart << numBits) | lowPart, numBits * 2);

            long quotient = dividend / signedDivisor;
            long remainder = dividend % signedDivisor;

            long minVal = -(1L << (numBits - 1));
            long maxVal = (1L << (numBits - 1)) - 1;
            if (quotient < minVal || quotient > maxVal) {
                throw new ArithmeticException("IDIV: quotient overflow (doesn't fit in destination)");
            }

            state.setRegister("rax", size.getBytes(), quotient & mask);
            state.setRegister("rdx", size.getBytes(), remainder & mask);
        }

        state.nextInstruction();
    }

    /**
     * Sign-extends a value from a specified bit-width to 64 bits.
     *
     * @param value The value to be sign-extended.
     * @param bits  The number of bits
     * @return The sign-extended 64-bit value.
     */
    private static long signExtend(long value, int bits) {
        long shift = 64 - bits;
        return (value << shift) >> shift;
    }

    /**
     * Converts a signed long to an unsigned BigInteger
     *
     * @param value The signed value to be converted
     * @return A BigInteger representing the unsigned value
     */
    private static BigInteger toUnsignedBigInt(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        String srcDesc = operands.get(0).getDescription(state, labelManager);
        return switch (size) {
            case BYTE -> "Divided (signed) AX by " + srcDesc +
                    ". The quotient was stored in %al, and the remainder in %ah";

            case WORD -> "Divided (signed) the combined DX:AX value by " + srcDesc +
                    ". The quotient was stored in AX, and the remainder in DX";

            case LONG -> "Divided (signed) the combined EDX:EAX value by " + srcDesc +
                    ". The quotient was stored in EAX, and the remainder in EDX";

            case QUAD -> "Divided (signed) the combined RDX:RAX value by " + srcDesc +
                    ". The quotient was stored in RAX, and the remainder in RDX";
        };
    }
}
