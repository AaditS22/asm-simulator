package io.github.AaditS22.asmvisualizer.backend.cpu;

import io.github.AaditS22.asmvisualizer.backend.util.MemoryLayout;

import java.util.HashMap;
import java.util.Map;

public class CPUState {
    private final Memory memory;
    private final Flags flags;
    private Map<String, Long> registers;

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
        registers.put("rip", MemoryLayout.CODE_BASE);
        for (int i = 8; i < 16; i++) {
            registers.put("r" + i, 0L);
        }
    }

    /**
     * Constructor for a new CPUState
     */
    public CPUState() {
        initializeRegisters();
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
        if (key.equals("rip")) {
            throw new IllegalArgumentException("%rip is read-only!");
        }
        long oldFull = registers.get(key);
        long m = mask(bytes);
        long newValue = (oldFull & ~m) | (value & m);
        registers.put(key, newValue);
    }

    /**
     * Sets the program counter (%rip) to a specific value (used in jumps)
     * @param address the address of the instruction to jump to
     */
    public void setPC(long address) {
        registers.put("rip", address);
    }

    /**
     * Getter for the current value of the program counter (%rip)
     * @return the current value of the PC as an instruction address
     */
    public long getPC() {
        return registers.get("rip");
    }

    /**
     * Increments the PC by 8 to move to the next instruction
     */
    public void nextInstruction() {
        registers.put("rip", registers.get("rip") + MemoryLayout.INSTRUCTION_SIZE);
    }

    /**
     * Fully restarts the program, bringing CPUState back to its original config
     */
    public void restartProgram() {
        initializeRegisters();
        memory.clearMemory();
        flags.resetFlags();
    }
}
