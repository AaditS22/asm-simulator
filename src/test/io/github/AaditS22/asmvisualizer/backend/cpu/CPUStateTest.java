package io.github.AaditS22.asmvisualizer.backend.cpu;

import io.github.AaditS22.asmvisualizer.backend.util.MemoryLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUStateTest {
    private CPUState cpuState;

    @BeforeEach
    void setUp() {
        cpuState = new CPUState();
    }

    @Test
    void initializeTest() {
        assertEquals(0L, cpuState.getRegister("rax", 8));
        assertEquals(0L, cpuState.getRegister("rbx", 8));
        assertEquals(0L, cpuState.getRegister("r9", 8));
        assertEquals(0L, cpuState.getRegister("r15", 8));
    }

    @Test
    void getSetRegisterTest() {
        cpuState.setRegister("rax", 8, 0xABCDABCDABCDABCDL);
        assertEquals(0xABCDABCDABCDABCDL, cpuState.getRegister("rax", 8));
        cpuState.setRegister("rax", 2, 0xEFEFL);
        assertEquals(0xEFEFL, cpuState.getRegister("rax", 2));
        assertEquals(0xABCDABCDABCDEFEFL, cpuState.getRegister("rax", 8));
        cpuState.setRegister("rax", 4, 0xEFEFL);
        assertEquals(0xEFEFL, cpuState.getRegister("rax", 8));
    }

    @Test
    void programCounterTest() {
        assertEquals(MemoryLayout.CODE_BASE, cpuState.getPC());
        cpuState.nextInstruction();
        assertEquals(MemoryLayout.CODE_BASE + 8, cpuState.getPC());
        cpuState.setPC(MemoryLayout.CODE_BASE + 64);
        assertEquals(MemoryLayout.CODE_BASE + 64, cpuState.getPC());
    }

    @Test
    void restartProgramTest() {
        cpuState.setRegister("rax", 8, 0xABCDABCDABCDABCDL);
        cpuState.nextInstruction();
        cpuState.getFlags().updateSubFlags(10, 10, 64);
        cpuState.getMemory().writeByte(0x100L, (byte) 50);
        cpuState.restartProgram();
        assertEquals(0L, cpuState.getRegister("rax", 8));
        assertEquals(MemoryLayout.CODE_BASE, cpuState.getPC());
        assertFalse(cpuState.getFlags().isZero());
        assertEquals((byte) 0, cpuState.getMemory().readByte(0x100L));
    }
}