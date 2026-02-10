package io.github.AaditS22.asmvisualizer.backend.cpu;

public class Flags {
    private boolean zeroFlag;
    private boolean signFlag;
    private boolean carryFlag;
    private boolean overflowFlag;

    public Flags() {
        zeroFlag = false;
        signFlag = false;
        carryFlag = false;
        overflowFlag = false;
    }

    // Getters for all flags

    public boolean isZero() {
        return zeroFlag;
    }

    public boolean isNegative() {
        return signFlag;
    }

    public boolean isCarry() {
        return carryFlag;
    }

    public boolean isOverflow() {
        return overflowFlag;
    }

    // Helper methods to update flags after certain operations

    /**
     * Updates all flags after an add operation
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param numBits the number of bits in the operation
     */
    public void updateAddFlags(long operand1, long operand2, int numBits) {
        // Gives a sequence of 1s that is the length of numBits to properly constrain the result
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        // Gives a mask to check if the sign bit is 1 or 0
        long signMask = 1L << (numBits - 1);

        long result = (operand1 + operand2) & mask;
        zeroFlag = (result == 0);
        signFlag = (result & signMask) != 0;
        // Check if the result is less than an operand (unsigned because overflow mask is for signed)
        carryFlag = Long.compareUnsigned(result, operand1 & mask) < 0;

        boolean op1Sign = (operand1 & signMask) != 0;
        boolean op2Sign = (operand2 & signMask) != 0;
        boolean resSign = (result & signMask) != 0;
        overflowFlag = (op1Sign == op2Sign) && (op1Sign != resSign);
    }

    /**
     * Updates all flags after a sub/cmp operation
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param numBits the number of bits in the operation
     */
    public void updateSubFlags(long operand1, long operand2, int numBits) {
        // Gives a sequence of 1s that is the length of numBits to properly constrain the result
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        // Gives a mask to check if the sign bit is 1 or 0
        long signMask = 1L << (numBits - 1);

        long result = (operand1 - operand2) & mask;
        zeroFlag = (result == 0);
        signFlag = (result & signMask) != 0;
        // Check if the first operand was less than the second (meaning it had to carry over)
        carryFlag = Long.compareUnsigned(operand1 & mask, operand2 & mask) < 0;

        boolean op1Sign = (operand1 & signMask) != 0;
        boolean op2Sign = (operand2 & signMask) != 0;
        boolean resSign = (result & signMask) != 0;
        overflowFlag = (op1Sign != op2Sign) && (op1Sign != resSign);
    }

    /**
     * Updates flags after a logical (AND/OR/NOR/XOR) operation
     * @param result the result of the operation
     * @param numBits the number of bits in the operation
     */
    public void updateLogicalFlags(long result, int numBits) {
        long signMask = 1L << (numBits - 1);
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        result &= mask;
        zeroFlag = (result == 0);
        signFlag = (result & signMask) != 0;
        carryFlag = false;
        overflowFlag = false;
    }

    /**
     * Updates flags after an increment (similar to add but keeps CF the same)
     * @param operand the operand to increment
     * @param numBits the number of bits in the operation
     */
    public void updateIncFlags(long operand, int numBits) {
        boolean oldCarryFlag = carryFlag;
        updateAddFlags(operand, 1, numBits);
        carryFlag = oldCarryFlag;
    }

    /**
     * Updates flags after a decrement (similar to sub but keeps CF the same)
     * @param operand the operand to decrement
     * @param numBits the number of bits in the operation
     */
    public void updateDecFlags(long operand, int numBits) {
        boolean oldCarryFlag = carryFlag;
        updateSubFlags(operand, 1, numBits);
        carryFlag = oldCarryFlag;
    }

    /**
     * Updates flags after a negation (similar to sub by carryFlag changes based on if operand is 0 or not)
     * @param operand the operand to negate
     * @param numBits the number of bits in the operation
     */
    public void updateNegateFlags(long operand, int numBits) {
        updateSubFlags(0, operand, numBits);
        long mask = (numBits == 64) ? -1L : (1L << numBits) - 1;
        carryFlag = (operand & mask) != 0;
    }
}
