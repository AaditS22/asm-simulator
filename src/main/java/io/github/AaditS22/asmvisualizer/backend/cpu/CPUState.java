package io.github.AaditS22.asmvisualizer.backend.cpu;

import java.util.HashMap;
import java.util.Map;

public class CPUState {
    private final Memory memory;
    private final Flags flags;
    private Map<String, Long> registers;
    private int programCounter;

    /**
     * Helper method to initialize all important registers
     */
    private void initializeRegisters() {
        registers = new HashMap<>();
        registers.put("rax", 0L);
        registers.put("rbx", 0L);
        registers.put("rcx", 0L);
        registers.put("rdx", 0L);
        registers.put("rsp", 0x7FFFFFFFF000L);
        registers.put("rbp", 0x7FFFFFFFF000L);
        registers.put("rsi", 0L);
        registers.put("rdi", 0L);
        for (int i = 8; i < 16; i++) {
            registers.put("r" + i, 0L);
        }
    }

    /**
     * Constructor for a new CPUState
     */
    public CPUState() {
        initializeRegisters();
        programCounter = 0;
        memory = new Memory(registers);
        flags = new Flags();
    }

    /**
     * Helper method to create a mask to get certain parts of a register
     * @param bytes the number of bytes to access
     * @return a mask for a value to only show the number of bytes
     */
    private static long mask(int bytes) {
        return bytes == 8 ? -1L : (1L << (bytes * 8)) - 1;
    }

    /**
     * Getter for memory object
     * @return the saved memory object
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Getter for flags object
     * @return the saved flags object
     */
    public Flags getFlags() {
        return flags;
    }

    /**
     * Gets the value of a specific register/sub-register
     * @param registerName the name of the register
     * @param bytes the number of bytes to get, corresponding to the sub-register
     * @return the value stored
     */
    public long getRegister(String registerName, int bytes) {
        long full = registers.get(registerName.toLowerCase());
        return full & mask(bytes);
    }

    /**
     * Sets the value of a register/sub-register
     * @param registerName the name of the register
     * @param bytes the number of bytes corresponding to a sub-register
     * @param value the value to set it to
     */
    public void setRegister(String registerName, int bytes, long value) {
        String key = registerName.toLowerCase();
        long oldFull = registers.get(key);
        long m = mask(bytes);
        long newValue = (oldFull & ~m) | (value & m);
        registers.put(key, newValue);
    }

    /**
     * Sets the PC to a specific value (used in jumps)
     * @param programCounter the value to set it to
     */
    public void setPC(int programCounter) {
        this.programCounter = programCounter;
    }

    /**
     * Getter for the current value of the program counter
     * @return the current value of the PC
     */
    public int getPC() {
        return programCounter;
    }

    /**
     * Increments the PC by 1 to move to the next instruction
     */
    public void nextInstruction() {
        programCounter++;
    }

    /**
     * Fully restarts the program, bringing CPUState back to its original config
     */
    public void restartProgram() {
        programCounter = 0;
        initializeRegisters();
        memory.clearMemory();
        flags.resetFlags();
    }
}
