package io.github.AaditS22.asmsimulator.backend;

import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.StepResult;
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

        StepResult result = sim.step();

        assertNotNull(result.description());
        assertFalse(result.description().isBlank());
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
        StepResult result = sim.step();
        assertNotNull(result.description());
        assertEquals(10L, sim.getState().getRegister("rax", 8));
    }

    @Test
    void stepOutputEmptyForNonIoInstruction() {
        sim.load("""
                .text
                main:
                    movq $1, %rax
                    ret
                """);
        StepResult result = sim.step();
        assertFalse(result.hasOutput());
        assertEquals("", result.output());
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
        sim.step();
        sim.step();
        sim.step();
        assertThrows(Exception.class, () -> sim.step());
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

        List<StepResult> results = sim.runAll(100);

        assertEquals(3, results.size());
        assertTrue(sim.isHalted());
        assertEquals(30L, sim.getState().getRegister("rbx", 8));
    }

    @Test
    void runAllDescriptionsNonBlank() {
        sim.load("""
                .text
                main:
                    movq $1, %rax
                    movq $2, %rbx
                """);

        List<StepResult> results = sim.runAll(100);
        results.forEach(r -> assertFalse(r.description().isBlank()));
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

    // ── printf integration ───────────────────────────────────────────────────

    @Test
    void printfOutputCapturedInStepResult() {
        sim.load("""
                .rodata
                fmt: .asciz "hello"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        sim.step(); // leaq
        StepResult result = sim.step(); // call printf

        assertTrue(result.hasOutput());
        assertEquals("hello", result.output());
    }

    @Test
    void printfDoesNotCorruptRsp() {
        sim.load("""
                .rodata
                fmt: .asciz "x"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        long rspBefore = sim.getState().getRegister("rsp", 8);
        sim.step(); // leaq
        sim.step(); // call printf
        long rspAfter = sim.getState().getRegister("rsp", 8);

        assertEquals(rspBefore, rspAfter);
    }

    @Test
    void printfOverwritesStackSlotDuringExecution() {
        sim.load("""
                .rodata
                fmt: .asciz "x"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        long rsp = sim.getState().getRegister("rsp", 8);
        long sentinel = 0xDEADBEEFL;
        sim.getState().getMemory().writeQuad(rsp - 8, sentinel);

        sim.step(); // leaq — stack slot untouched
        assertEquals(sentinel, sim.getState().getMemory().readQuad(rsp - 8));

        sim.step(); // call printf — return address is pushed then popped
        // After the call the slot should contain the return address that was pushed,
        // not the sentinel — confirming the push occurred and overwrote it
        long returnAddress = MemoryLayout.CODE_BASE + 2 * MemoryLayout.INSTRUCTION_SIZE;
        assertEquals(returnAddress, sim.getState().getMemory().readQuad(rsp - 8));
    }

    @Test
    void printfWithIntArgProducesCorrectOutput() {
        sim.load("""
                .rodata
                fmt: .asciz "n=%d"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    movq $7, %rsi
                    call printf
                    ret
                """);

        sim.step(); // leaq
        sim.step(); // movq
        StepResult result = sim.step(); // call printf

        assertEquals("n=7", result.output());
    }

    @Test
    void nonPrintfCallHasNoOutput() {
        sim.load("""
                .text
                .globl main
                main:
                    call func
                    ret
                func:
                    movq $1, %rax
                    ret
                """);

        StepResult result = sim.step(); // call func
        assertFalse(result.hasOutput());
    }

    @Test
    void printfOutputClearedBetweenSteps() {
        sim.load("""
                .rodata
                fmt: .asciz "hi"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    movq $0, %rax
                    ret
                """);

        sim.step();
        StepResult printf = sim.step();
        StepResult mov = sim.step();

        assertEquals("hi", printf.output());
        assertEquals("", mov.output());
    }

    @Test
    void printfOutputClearedOnReset() {
        sim.load("""
                .rodata
                fmt: .asciz "hi"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        sim.step();
        sim.step(); // printf writes to buffer, step() flushes it into StepResult

        // Manually write to buffer to simulate unflushed state, then reset
        sim.getState().getIOBuffer().append("leftover");
        sim.reset();

        assertTrue(sim.getState().getIOBuffer().isEmpty());
    }

    @Test
    void exitHaltsSimulator() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $0, %rdi
                    call exit
                """);

        sim.step(); // movq
        sim.step(); // call exit
        assertTrue(sim.isHalted());
    }

    @Test
    void exitWithZeroCode() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $0, %rdi
                    call exit
                """);

        sim.step();
        sim.step();
        assertEquals(0L, sim.getExitCode());
    }

    @Test
    void exitWithNonZeroCode() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $42, %rdi
                    call exit
                """);

        sim.step();
        sim.step();
        assertEquals(42L, sim.getExitCode());
    }

    @Test
    void exitDoesNotRequireRet() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $1, %rdi
                    call exit
                    movq $99, %rax
                """);

        sim.step();
        sim.step(); // call exit — should halt here
        assertTrue(sim.isHalted());
        // The movq after exit must never execute
        assertEquals(0L, sim.getState().getRegister("rax", 8));
    }

    @Test
    void exitDoesNotModifyRsp() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $0, %rdi
                    call exit
                """);

        long rspBefore = sim.getState().getRegister("rsp", 8);
        sim.step();
        sim.step();
        assertEquals(rspBefore, sim.getState().getRegister("rsp", 8));
    }

    @Test
    void exitDescriptionContainsCodeAndConvention() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $0, %rdi
                    call exit
                """);

        sim.step(); // movq
        StepResult result = sim.step(); // call exit
        assertTrue(result.description().contains("exit"));
        assertTrue(result.description().contains("0"));
        assertTrue(result.description().contains("success") || result.description().contains("0"));
    }

    @Test
    void getExitCodeBeforeHaltThrows() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $0, %rdi
                    call exit
                """);

        assertThrows(IllegalStateException.class, () -> sim.getExitCode());
    }

    @Test
    void exitCodeIsZeroByDefaultOnNaturalHalt() {
        sim.load("""
                .text
                .globl main
                main:
                    nop
                """);

        sim.step();
        assertTrue(sim.isHalted());
        assertEquals(0L, sim.getExitCode());
    }

    @Test
    void exitCodeResetOnReload() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $7, %rdi
                    call exit
                """);

        sim.step();
        sim.step();
        assertEquals(7L, sim.getExitCode());

        sim.reset();
        sim.step();
        sim.step();
        assertEquals(7L, sim.getExitCode()); // same program, same code
    }

    @Test
    void exitAfterPrintf() {
        sim.load("""
                .rodata
                fmt: .asciz "done\\n"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    movq $0, %rdi
                    call exit
                """);

        sim.step(); // leaq
        StepResult printResult = sim.step(); // printf
        assertEquals("done\n", printResult.output());

        sim.step(); // movq
        sim.step(); // exit
        assertTrue(sim.isHalted());
        assertEquals(0L, sim.getExitCode());
    }

    @Test
    void runAllStopsAtExit() {
        sim.load("""
                .text
                .globl main
                main:
                    movq $3, %rdi
                    call exit
                    movq $99, %rax
                """);

        var results = sim.runAll(100);
        assertTrue(sim.isHalted());
        assertEquals(3L, sim.getExitCode());
        // movq $99 must never have run
        assertEquals(0L, sim.getState().getRegister("rax", 8));
        // Only movq $3 and call exit ran
        assertEquals(2, results.size());
    }
}