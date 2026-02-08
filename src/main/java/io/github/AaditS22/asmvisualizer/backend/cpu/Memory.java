package io.github.AaditS22.asmvisualizer.backend.cpu;

import java.util.HashMap;
import java.util.Map;

public class Memory {
    private final Map<Long, Byte> data = new HashMap<>();
    private final Map<String, Long> registers;

    // Reference to the starting address of the user's stack view (not %rsp or %rbp)
    private Long stackViewStart;

    /**
     * Constructor for memory object
     * @param registers a reference to the CPU's registers
     */
    public Memory(Map<String, Long> registers) {
        // Default stack start is same as default %rbp
        this.stackViewStart = 0x7FFFFFFFF000L;
        this.registers = registers;
    }

    // Read methods

    /**
     * Helper method to read N bytes from memory
     * @param address the address to read from
     * @param bytes the number of bytes to read
     * @return the result (temporarily in long format)
     */
    private long readN(long address, int bytes) {
        long value = 0;
        for (int i = 0; i < bytes; i++) {
            // Non-existing addresses default to 0, AND operation with 0xFF done to unsign the byte
            value |= ((long) data.getOrDefault(address + i, (byte) 0) & 0xFF) << (8 * i);
        }
        return value;
    }

    /**
     * Reads a byte from memory
     * @param address the address to read from
     * @return the byte at the address (or 0 as the default value)
     */
    public Byte readByte(long address) {
        return data.getOrDefault(address, (byte) 0);
    }

    /**
     * Reads a word from memory
     * @param address the address to read from
     * @return a combination of the byte at the address and the next one
     */
    public Short readWord(long address) {
        return (short) readN(address, 2);
    }

    /**
     * Reads a long from memory
     * @param address the address to read from
     * @return a combination of the 4 bytes starting from the address
     */
    public Integer readLong(long address) {
        return (int) readN(address, 4);
    }

    /**
     * Reads a quad from memory
     * @param address the address to read from
     * @return a combination of the 8 bytes starting from the address
     */
    public Long readQuad(long address) {
        return readN(address, 8);
    }

    // Write methods

    /**
     * Helper method to write N bytes to memory
     * @param address the starting address to write at
     * @param value the value to write
     * @param bytes the number of bytes to write
     */
    private void writeN(long address, long value, int bytes) {
        for (int i = 0; i < bytes; i++) {
            // Grabs the last byte of the value
            byte b = (byte) (value & 0xFF);
            data.put(address + i, b);
            // Shifts by 8 bits to place the next byte at the end
            value >>= 8;
        }
    }

    /**
     * Writes a byte to memory
     * @param address the address to write at
     * @param value the value to write
     */
    public void writeByte(long address, byte value) {
        data.put(address, value);
    }

    /**
     * Writes a word to memory
     * @param address the address to write at
     * @param value the value (as short) to write
     */
    public void writeWord(long address, short value) {
        writeN(address, value, 2);
    }

    /**
     * Writes a long to memory
     * @param address the address to write at
     * @param value the value (as int) to write
     */
    public void writeLong(long address, int value) {
        writeN(address, value, 4);
    }

    /**
     * Writes a quad to memory
     * @param address the address to write at
     * @param value the value (as long) to write
     */
    public void writeQuad(long address, long value) {
        writeN(address, value, 8);
    }

    // Stack methods

    /**
     * Gets the current state of the stack
     * @return the first 10 stored quads, starting from the stored stack start and
     * going to lower addresses
     */
    public Long[] getStack() {
        Long[] stack = new Long[10];
        for (int i = 0; i < 10; i++) {
            stack[i] = readQuad(stackViewStart - (i * 8));
        }
        return stack;
    }

    /**
     * Pushes a quad to the stack
     * @param value the quad to push
     * @return an updated representation of the stack
     */
    public Long[] stackPush(long value) {
        long rsp = registers.get("rsp") - 8;
        registers.put("rsp", rsp);
        writeQuad(rsp, value);
        return getStack();
    }

    /**
     * Pops a value from the stack, moving %rsp up by 8
     * @return the value at the previous rsp
     */
    public Long stackPop() {
        long rsp = registers.get("rsp");
        long result = readQuad(rsp);
        registers.put("rsp", rsp + 8);
        return result;
    }

    /**
     * Setter for stackViewStart
     * @param stackViewStart the value to set it to
     */
    public void setStackViewStart(long stackViewStart) {
        this.stackViewStart = stackViewStart;
    }

    /**
     * Getter for stackViewStart
     * @return the current value as a long
     */
    public long getStackViewStart() {
        return stackViewStart;
    }

    // Other util methods

    /**
     * Clears the data in the memory
     */
    public void clearMemory() {
        data.clear();
    }
}
