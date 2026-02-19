package io.github.AaditS22.asmsimulator.backend;

import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// DISCLAIMER: Many of the following tests were written with the help of LLMs
class SimulatorTest {

    private Simulator sim;

    @BeforeEach
    void setUp() {
        sim = new Simulator();
    }

    // ── Load / lifecycle ─────────────────────────────────────────────────────

    @Test
    void loadSetsReadyStatus() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $1, %rax
                    ret
                """);
        assertEquals(Simulator.Status.READY, sim.getStatus());
        assertEquals(2, sim.getTotalInstructions());
    }

    @Test
    void loadEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> sim.load("   "));
    }

    @Test
    void stepBeforeLoadThrows() {
        assertThrows(IllegalStateException.class, () -> sim.step());
    }

    @Test
    void resetBeforeLoadThrows() {
        assertThrows(IllegalStateException.class, () -> sim.reset());
    }

    // ── PC → index conversion ────────────────────────────────────────────────

    @Test
    void initialPcPointsToEntryLabel() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $5, %rax
                    ret
                """);
        assertEquals(MemoryLayout.CODE_BASE, sim.getState().getPC());
        assertEquals(0, sim.getCurrentInstructionIndex());
    }

    @Test
    void entryLabelOffsetCorrect() {
        sim.load("""
                .text
                .globl entry
                    nop
                entry:
                    movq $7, %rax
                    ret
                """);
        long expected = MemoryLayout.CODE_BASE + MemoryLayout.INSTRUCTION_SIZE;
        assertEquals(expected, sim.getState().getPC());
        assertEquals(1, sim.getCurrentInstructionIndex());
    }

    // ── Single step ──────────────────────────────────────────────────────────

    @Test
    void stepExecutesInstructionAndAdvancesPC() {
        sim.load("""
                .text
                main:
                    movq $42, %rax
                    movq $7,  %rbx
                    ret
                """);

        String description = sim.step();

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertEquals(42L, sim.getState().getRegister("rax", 8));
        assertEquals(MemoryLayout.CODE_BASE + 8, sim.getState().getPC());
    }

    @Test
    void stepReturnsDescriptionFromPreExecutionState() {
        sim.load("""
                .text
                main:
                    movq $10, %rax
                    ret
                """);
        String description = sim.step();
        assertNotNull(description);
        // rax should have been updated
        assertEquals(10L, sim.getState().getRegister("rax", 8));
    }

    @Test
    void stepAfterHaltThrows() {
        sim.load("""
                .text
                main:
                    nop
                """);
        sim.step();
        assertTrue(sim.isHalted());
        assertThrows(IllegalStateException.class, () -> sim.step());
    }

    // ── Halt conditions ──────────────────────────────────────────────────────

    @Test
    void programHaltsNaturallyWhenPcPassesEnd() {
        sim.load("""
                .text
                main:
                    movq $1, %rax
                """);
        sim.step();
        assertEquals(Simulator.Status.HALTED, sim.getStatus());
    }

    @Test
    void divByZeroThrows() {
        sim.load("""
                .text
                main:
                    movq $0, %rcx
                    movq $5, %rax
                    movq $0, %rdx
                    idivq %rcx
                    ret
                """);
        sim.step(); // movq $0 → rcx
        sim.step(); // movq $5 → rax
        sim.step(); // movq $0 → rdx
        assertThrows(Exception.class, () -> sim.step()); // idivq %rcx → divide by zero
    }

    // ── runAll ───────────────────────────────────────────────────────────────

    @Test
    void runAllExecutesAllInstructions() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $10, %rax
                    movq $20, %rbx
                    addq %rax, %rbx
                """);

        List<String> descriptions = sim.runAll(100);

        assertEquals(3, descriptions.size());
        assertTrue(sim.isHalted());
        assertEquals(30L, sim.getState().getRegister("rbx", 8));
    }

    @Test
    void runAllThrowsOnStepLimitExceeded() {
        sim.load("""
                .text
                main:
                    jmp main
                """);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> sim.runAll(10));
        assertTrue(e.getMessage().contains("10"));
    }

    @Test
    void runAllBeforeLoadThrows() {
        assertThrows(IllegalStateException.class, () -> sim.runAll(100));
    }

    @Test
    void runAllWhenHaltedThrows() {
        sim.load("""
                .text
                main:
                    nop
                """);
        sim.step();
        assertThrows(IllegalStateException.class, () -> sim.runAll(100));
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    @Test
    void resetRestoresInitialState() {
        sim.load("""
                .text
                main:
                    movq $99, %rax
                    movq $88, %rbx
                """);

        sim.runAll(100);
        assertTrue(sim.isHalted());

        sim.reset();

        assertFalse(sim.isHalted());
        assertEquals(Simulator.Status.READY, sim.getStatus());
        assertEquals(0L, sim.getState().getRegister("rax", 8));
        assertEquals(0L, sim.getState().getRegister("rbx", 8));
        assertEquals(MemoryLayout.CODE_BASE, sim.getState().getPC());
    }

    @Test
    void resetReloadsDataSection() {
        sim.load("""
                .data
                val: .quad 55
                .text
                main:
                    movq val, %rax
                """);

        sim.step();
        assertEquals(55L, sim.getState().getRegister("rax", 8));

        sim.getState().getMemory().writeQuad(MemoryLayout.DATA_BASE, 0L);
        sim.reset();

        sim.step();
        assertEquals(55L, sim.getState().getRegister("rax", 8));
    }

    // ── getCurrentInstruction ────────────────────────────────────────────────

    @Test
    void getCurrentInstructionMatchesPC() {
        sim.load("""
                .text
                main:
                    movq $1, %rax
                    movq $2, %rbx
                    ret
                """);

        assertNotNull(sim.getCurrentInstruction());
        assertEquals("movq", sim.getCurrentInstruction().getMnemonic());

        sim.step();
        assertEquals("movq", sim.getCurrentInstruction().getMnemonic());

        sim.step();
        assertEquals("ret", sim.getCurrentInstruction().getMnemonic());
    }

    @Test
    void getCurrentInstructionReturnsNullWhenHalted() {
        sim.load("""
                .text
                main:
                    nop
                """);
        sim.step();
        assertTrue(sim.isHalted());
        assertNull(sim.getCurrentInstruction());
    }
}