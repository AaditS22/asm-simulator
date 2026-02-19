package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.Simulator;
import io.github.AaditS22.asmsimulator.backend.util.StepResult;
import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.PrintfHandler;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// DISCLAIMER: Most of the following tests were written with the help of LLMs
class PrintfHandlerTest {

    private CPUState state;
    private Simulator sim;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        sim = new Simulator();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void writeString(long address, String s) {
        for (int i = 0; i < s.length(); i++) {
            state.getMemory().writeByte(address + i, (byte) s.charAt(i));
        }
        state.getMemory().writeByte(address + s.length(), (byte) 0);
    }

    private void setFormatString(String format) {
        long addr = MemoryLayout.READ_ONLY_DATA_BASE;
        writeString(addr, format);
        state.setRegister("rdi", 8, addr);
    }

    // ── Null format string ────────────────────────────────────────────────────

    @Test
    void nullFormatStringThrows() {
        state.setRegister("rdi", 8, 0L);
        assertThrows(IllegalArgumentException.class, () -> PrintfHandler.execute(state));
    }

    // ── Plain string (no specifiers) ──────────────────────────────────────────

    @Test
    void plainStringNoSpecifiers() {
        setFormatString("Hello, World!\n");
        PrintfHandler.execute(state);
        assertEquals("Hello, World!\n", state.getIOBuffer().flush());
    }

    @Test
    void setsRaxToCharCount() {
        setFormatString("Hi");
        PrintfHandler.execute(state);
        assertEquals(2L, state.getRegister("rax", 8));
    }

    // ── %d / %i ───────────────────────────────────────────────────────────────

    @Test
    void percentD_positiveInt() {
        setFormatString("%d");
        state.setRegister("rsi", 8, 42L);
        PrintfHandler.execute(state);
        assertEquals("42", state.getIOBuffer().flush());
    }

    @Test
    void percentD_negativeInt() {
        setFormatString("%d");
        state.setRegister("rsi", 8, -1L);
        PrintfHandler.execute(state);
        assertEquals("-1", state.getIOBuffer().flush());
    }

    @Test
    void percentD_zero() {
        setFormatString("%d");
        state.setRegister("rsi", 8, 0L);
        PrintfHandler.execute(state);
        assertEquals("0", state.getIOBuffer().flush());
    }

    @Test
    void percentLd_largeValue() {
        setFormatString("%ld");
        state.setRegister("rsi", 8, Long.MAX_VALUE);
        PrintfHandler.execute(state);
        assertEquals(String.valueOf(Long.MAX_VALUE), state.getIOBuffer().flush());
    }

    // ── %u ────────────────────────────────────────────────────────────────────

    @Test
    void percentU_unsignedPositive() {
        setFormatString("%u");
        state.setRegister("rsi", 8, 300L);
        PrintfHandler.execute(state);
        assertEquals("300", state.getIOBuffer().flush());
    }

    @Test
    void percentU_allOnes_isMaxUnsigned() {
        setFormatString("%u");
        state.setRegister("rsi", 8, 0xFFFFFFFFL);
        PrintfHandler.execute(state);
        assertEquals("4294967295", state.getIOBuffer().flush());
    }

    // ── %x / %X ───────────────────────────────────────────────────────────────

    @Test
    void percentX_lowercase() {
        setFormatString("%x");
        state.setRegister("rsi", 8, 255L);
        PrintfHandler.execute(state);
        assertEquals("ff", state.getIOBuffer().flush());
    }

    @Test
    void percentX_uppercase() {
        setFormatString("%X");
        state.setRegister("rsi", 8, 255L);
        PrintfHandler.execute(state);
        assertEquals("FF", state.getIOBuffer().flush());
    }

    @Test
    void percentX_hashFlag() {
        setFormatString("%#x");
        state.setRegister("rsi", 8, 255L);
        PrintfHandler.execute(state);
        assertEquals("0xff", state.getIOBuffer().flush());
    }

    // ── %c ────────────────────────────────────────────────────────────────────

    @Test
    void percentC_char() {
        setFormatString("%c");
        state.setRegister("rsi", 8, (long) 'A');
        PrintfHandler.execute(state);
        assertEquals("A", state.getIOBuffer().flush());
    }

    // ── %s ────────────────────────────────────────────────────────────────────

    @Test
    void percentS_string() {
        long strAddr = MemoryLayout.DATA_BASE;
        writeString(strAddr, "hello");
        setFormatString("%s");
        state.setRegister("rsi", 8, strAddr);
        PrintfHandler.execute(state);
        assertEquals("hello", state.getIOBuffer().flush());
    }

    @Test
    void percentS_withPrecision() {
        long strAddr = MemoryLayout.DATA_BASE;
        writeString(strAddr, "hello");
        setFormatString("%.3s");
        state.setRegister("rsi", 8, strAddr);
        PrintfHandler.execute(state);
        assertEquals("hel", state.getIOBuffer().flush());
    }

    @Test
    void percentS_nullPointerThrows() {
        setFormatString("%s");
        state.setRegister("rsi", 8, 0L);
        assertThrows(IllegalArgumentException.class, () -> PrintfHandler.execute(state));
    }

    // ── %% ────────────────────────────────────────────────────────────────────

    @Test
    void percentPercent_literal() {
        setFormatString("100%%");
        PrintfHandler.execute(state);
        assertEquals("100%", state.getIOBuffer().flush());
    }

    // ── Width and padding ─────────────────────────────────────────────────────

    @Test
    void widthRightAlign() {
        setFormatString("%5d");
        state.setRegister("rsi", 8, 42L);
        PrintfHandler.execute(state);
        assertEquals("   42", state.getIOBuffer().flush());
    }

    @Test
    void widthLeftAlign() {
        setFormatString("%-5d");
        state.setRegister("rsi", 8, 42L);
        PrintfHandler.execute(state);
        assertEquals("42   ", state.getIOBuffer().flush());
    }

    @Test
    void widthZeroPad() {
        setFormatString("%05d");
        state.setRegister("rsi", 8, 42L);
        PrintfHandler.execute(state);
        assertEquals("00042", state.getIOBuffer().flush());
    }

    @Test
    void plusFlagPositive() {
        setFormatString("%+d");
        state.setRegister("rsi", 8, 42L);
        PrintfHandler.execute(state);
        assertEquals("+42", state.getIOBuffer().flush());
    }

    // ── Multiple args ─────────────────────────────────────────────────────────

    @Test
    void multipleArgsAllRegisters() {
        setFormatString("%d %d %d %d %d");
        state.setRegister("rsi", 8, 1L);
        state.setRegister("rdx", 8, 2L);
        state.setRegister("rcx", 8, 3L);
        state.setRegister("r8", 8, 4L);
        state.setRegister("r9", 8, 5L);
        PrintfHandler.execute(state);
        assertEquals("1 2 3 4 5", state.getIOBuffer().flush());
    }

    @Test
    void stackArgsAfterRegisters() {
        setFormatString("%d %d %d %d %d %d");
        state.setRegister("rsi", 8, 1L);
        state.setRegister("rdx", 8, 2L);
        state.setRegister("rcx", 8, 3L);
        state.setRegister("r8", 8, 4L);
        state.setRegister("r9", 8, 5L);
        long rsp = state.getRegister("rsp", 8);
        state.getMemory().writeQuad(rsp, 6L);
        PrintfHandler.execute(state);
        assertEquals("1 2 3 4 5 6", state.getIOBuffer().flush());
    }

    // ── Unsupported specifiers ────────────────────────────────────────────────

    @Test
    void floatSpecifierThrows() {
        setFormatString("%f");
        state.setRegister("rsi", 8, 0L);
        assertThrows(UnsupportedOperationException.class, () -> PrintfHandler.execute(state));
    }

    @Test
    void percentNThrows() {
        setFormatString("%n");
        state.setRegister("rsi", 8, 0L);
        assertThrows(UnsupportedOperationException.class, () -> PrintfHandler.execute(state));
    }

    @Test
    void unknownSpecifierThrows() {
        setFormatString("%q");
        state.setRegister("rsi", 8, 0L);
        assertThrows(IllegalArgumentException.class, () -> PrintfHandler.execute(state));
    }

    // ── Integration via Simulator ─────────────────────────────────────────────

    @Test
    void simPrintfPlainString() {
        sim.load("""
                .rodata
                fmt: .asciz "Hello, World!\\n"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        sim.step(); // leaq
        StepResult result = sim.step(); // call printf
        assertEquals("Hello, World!\n", result.output());
        assertTrue(result.hasOutput());
    }

    @Test
    void simPrintfWithIntArg() {
        sim.load("""
                .rodata
                fmt: .asciz "Value: %d\\n"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    movq $42, %rsi
                    call printf
                    ret
                """);

        sim.step(); // leaq
        sim.step(); // movq
        StepResult result = sim.step(); // call printf
        assertEquals("Value: 42\n", result.output());
    }

    @Test
    void simPrintfDoesNotPushReturnAddress() {
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
        assertEquals(rspBefore, rspAfter, "printf should not modify rsp");
    }

    @Test
    void simPrintfSetsRax() {
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

        sim.step(); // leaq
        sim.step(); // call printf
        assertEquals(2L, sim.getState().getRegister("rax", 8));
    }

    @Test
    void simNonPrintfCallUnaffected() {
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
        assertEquals(2, sim.getCurrentInstructionIndex()); // jumped into func
    }

    @Test
    void simMultiplePrintfCallsAccumulate() {
        sim.load("""
                .rodata
                fmt: .asciz "x"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                    leaq fmt(%rip), %rdi
                    call printf
                    ret
                """);

        sim.step();
        StepResult r1 = sim.step(); // first printf
        sim.step();
        StepResult r2 = sim.step(); // second printf

        assertEquals("x", r1.output());
        assertEquals("x", r2.output());
    }

    @Test
    void simOutputClearedBetweenSteps() {
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
    void simRunAllCollectsOutput() {
        sim.load("""
                .rodata
                fmt: .asciz "done"
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    call printf
                """);

        List<StepResult> results = sim.runAll(100);
        long printfResults = results.stream().filter(StepResult::hasOutput).count();
        assertEquals(1, printfResults);
        String combined = results.stream().map(StepResult::output).reduce("", String::concat);
        assertEquals("done", combined);
    }
}