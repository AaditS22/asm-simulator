package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.Simulator;
import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.ScanfHandler;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// DISCLAIMER: Most of the following tests were written with the help of LLMs
class ScanfHandlerTest {

    private CPUState state;
    private Simulator sim;

    // ── Helpers ───────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        state = new CPUState();
        sim = new Simulator();
    }

    private void setFormatString(String format) {
        long addr = MemoryLayout.READ_ONLY_DATA_BASE;
        for (int i = 0; i < format.length(); i++) {
            state.getMemory().writeByte(addr + i, (byte) format.charAt(i));
        }
        state.getMemory().writeByte(addr + format.length(), (byte) 0);
        state.setRegister("rdi", 8, addr);
    }

    private long allocDestination(long address) {
        return address;
    }

    // ── Waiting for input ─────────────────────────────────────────────────────

    @Test
    void noInputSetsWaitingFlag() {
        setFormatString("%d");
        state.setRegister("rsi", 8, 0x5000L);
        ScanfHandler.execute(state);
        assertTrue(state.getIOBuffer().isWaitingForInput());
    }

    @Test
    void noInputDoesNotWriteToMemory() {
        setFormatString("%d");
        long dest = 0x5000L;
        state.getMemory().writeLong(dest, 0xDEADBEEF);
        state.setRegister("rsi", 8, dest);
        ScanfHandler.execute(state);
        assertEquals(0xDEADBEEF, state.getMemory().readLong(dest));
    }

    @Test
    void noInputDoesNotModifyRax() {
        setFormatString("%d");
        state.setRegister("rax", 8, 0xCAFEL);
        state.setRegister("rsi", 8, 0x5000L);
        ScanfHandler.execute(state);
        assertEquals(0xCAFEL, state.getRegister("rax", 8));
    }

    // ── %d / %i ───────────────────────────────────────────────────────────────

    @Test
    void percentD_positiveInt() {
        setFormatString("%d");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("42");
        ScanfHandler.execute(state);
        assertEquals(42, state.getMemory().readLong(dest));
        assertEquals(1L, state.getRegister("rax", 8));
    }

    @Test
    void percentD_negativeInt() {
        setFormatString("%d");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("-99");
        ScanfHandler.execute(state);
        assertEquals(-99, state.getMemory().readLong(dest));
        assertEquals(1L, state.getRegister("rax", 8));
    }

    @Test
    void percentD_skipsLeadingWhitespace() {
        setFormatString("%d");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("   7");
        ScanfHandler.execute(state);
        assertEquals(7, state.getMemory().readLong(dest));
    }

    // ── %ld ──────────────────────────────────────────────────────────────────

    @Test
    void percentLD_writesQuad() {
        setFormatString("%ld");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("9999999999");
        ScanfHandler.execute(state);
        assertEquals(9999999999L, state.getMemory().readQuad(dest));
    }

    // ── %hd ──────────────────────────────────────────────────────────────────

    @Test
    void percentHD_writesWord() {
        setFormatString("%hd");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("300");
        ScanfHandler.execute(state);
        assertEquals((short) 300, state.getMemory().readWord(dest));
    }

    // ── %hhd ─────────────────────────────────────────────────────────────────

    @Test
    void percentHHD_writesByte() {
        setFormatString("%hhd");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("65");
        ScanfHandler.execute(state);
        assertEquals((byte) 65, state.getMemory().readByte(dest));
    }

    // ── %u ───────────────────────────────────────────────────────────────────

    @Test
    void percentU_unsignedInt() {
        setFormatString("%u");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("3000000000");
        ScanfHandler.execute(state);
        // 3000000000 stored in int slot — check raw bits via readLong (unsigned read)
        assertEquals((int) 3000000000L, state.getMemory().readLong(dest));
    }

    // ── %s ───────────────────────────────────────────────────────────────────

    @Test
    void percentS_writesNullTerminatedString() {
        setFormatString("%s");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("hello");
        ScanfHandler.execute(state);
        assertEquals("hello", ScanfHandler.readNullTerminatedString(state.getMemory(), dest));
        assertEquals(1L, state.getRegister("rax", 8));
    }

    @Test
    void percentS_stopsAtWhitespace() {
        setFormatString("%s");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("hello world");
        ScanfHandler.execute(state);
        assertEquals("hello", ScanfHandler.readNullTerminatedString(state.getMemory(), dest));
    }

    @Test
    void percentS_withWidth() {
        setFormatString("%3s");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("hello");
        ScanfHandler.execute(state);
        assertEquals("hel", ScanfHandler.readNullTerminatedString(state.getMemory(), dest));
    }

    // ── %c ───────────────────────────────────────────────────────────────────

    @Test
    void percentC_readsSingleChar() {
        setFormatString("%c");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput("A");
        ScanfHandler.execute(state);
        assertEquals((byte) 'A', state.getMemory().readByte(dest));
        assertEquals(1L, state.getRegister("rax", 8));
    }

    @Test
    void percentC_doesNotSkipWhitespace() {
        setFormatString("%c");
        long dest = 0x5000L;
        state.setRegister("rsi", 8, dest);
        state.getIOBuffer().setInput(" A");
        ScanfHandler.execute(state);
        // %c should read the space, not skip it
        assertEquals((byte) ' ', state.getMemory().readByte(dest));
    }

    // ── Multiple specifiers ───────────────────────────────────────────────────

    @Test
    void multipleIntsFromRegisters() {
        setFormatString("%d %d %d");
        long dest1 = 0x5000L;
        long dest2 = 0x5010L;
        long dest3 = 0x5020L;
        state.setRegister("rsi", 8, dest1);
        state.setRegister("rdx", 8, dest2);
        state.setRegister("rcx", 8, dest3);
        state.getIOBuffer().setInput("1 2 3");
        ScanfHandler.execute(state);
        assertEquals(1, state.getMemory().readLong(dest1));
        assertEquals(2, state.getMemory().readLong(dest2));
        assertEquals(3, state.getMemory().readLong(dest3));
        assertEquals(3L, state.getRegister("rax", 8));
    }

    @Test
    void intAndStringTogether() {
        setFormatString("%d %s");
        long destInt = 0x5000L;
        long destStr = 0x5020L;
        state.setRegister("rsi", 8, destInt);
        state.setRegister("rdx", 8, destStr);
        state.getIOBuffer().setInput("42 hello");
        ScanfHandler.execute(state);
        assertEquals(42, state.getMemory().readLong(destInt));
        assertEquals("hello", ScanfHandler.readNullTerminatedString(state.getMemory(), destStr));
        assertEquals(2L, state.getRegister("rax", 8));
    }

    // ── rax match count ───────────────────────────────────────────────────────

    @Test
    void raxSetToMatchCount() {
        setFormatString("%d %d");
        state.setRegister("rsi", 8, 0x5000L);
        state.setRegister("rdx", 8, 0x5010L);
        state.getIOBuffer().setInput("10 20");
        ScanfHandler.execute(state);
        assertEquals(2L, state.getRegister("rax", 8));
    }

    // ── Null format string ────────────────────────────────────────────────────

    @Test
    void nullFormatStringThrows() {
        state.setRegister("rdi", 8, 0L);
        state.getIOBuffer().setInput("anything");
        assertThrows(IllegalArgumentException.class, () -> ScanfHandler.execute(state));
    }

    // ── Unsupported specifier ─────────────────────────────────────────────────

    @Test
    void floatSpecifierThrows() {
        setFormatString("%f");
        state.setRegister("rsi", 8, 0x5000L);
        state.getIOBuffer().setInput("3.14");
        assertThrows(UnsupportedOperationException.class, () -> ScanfHandler.execute(state));
    }

    // ── Integration via Simulator ─────────────────────────────────────────────

    @Test
    void simScanfPausesOnFirstStep() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step(); // leaq rdi
        sim.step(); // leaq rsi
        StepResult result = sim.step(); // call scanf — should pause
        assertTrue(sim.isWaitingForInput());
        assertFalse(result.description().isEmpty());
        assertTrue(result.description().contains("paused"));
    }

    @Test
    void simScanfResumeDescriptionDiffersFromPauseDescription() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        StepResult pause = sim.step(); // pauses
        sim.provideInput("5");
        StepResult resume = sim.step(); // completes
        assertNotEquals(pause.description(), resume.description());
        assertTrue(resume.description().contains("Resumes"));
    }

    @Test
    void simScanfWritesValueToMemory() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        sim.step(); // pause

        sim.provideInput("99");
        sim.step(); // complete

        // Read the value from the address that %rsi pointed to
        long valAddr = sim.getLabelManager().getDataLabel("val").address();
        assertEquals(99, sim.getState().getMemory().readLong(valAddr));
    }

    @Test
    void simScanfSetsRaxToMatchCount() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        sim.step();

        sim.provideInput("7");
        sim.step();

        assertEquals(1L, sim.getState().getRegister("rax", 8));
    }

    @Test
    void simScanfDoesNotAdvancePCOnPause() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        long pcBeforeScanf = sim.getState().getPC();
        sim.step(); // pause
        assertEquals(pcBeforeScanf, sim.getState().getPC());
    }

    @Test
    void simScanfAdvancesPCAfterResume() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        long pcBeforeScanf = sim.getState().getPC();
        sim.step();
        sim.provideInput("1");
        sim.step(); // resume
        assertTrue(sim.getState().getPC() > pcBeforeScanf);
    }

    @Test
    void simStepWhileWaitingThrows() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        sim.step();
        sim.step();
        sim.step(); // pause
        assertThrows(IllegalStateException.class, () -> sim.step());
    }

    @Test
    void simProvideInputWhenNotWaitingThrows() {
        sim.load("""
                .text
                .globl main
                main:
                    nop
                """);
        assertThrows(IllegalStateException.class, () -> sim.provideInput("hello"));
    }

    @Test
    void simScanfDoesNotCorruptRsp() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        long rspBefore = sim.getState().getRegister("rsp", 8);
        sim.step();
        sim.step();
        sim.step(); // pause
        assertEquals(rspBefore, sim.getState().getRegister("rsp", 8), "rsp must be unchanged after pause");
        sim.provideInput("3");
        sim.step(); // resume
        assertEquals(rspBefore, sim.getState().getRegister("rsp", 8), "rsp must be unchanged after resume");
    }

    @Test
    void simRunAllStopsAtScanf() {
        sim.load("""
                .rodata
                fmt: .asciz "%d"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq fmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    ret
                """);

        var results = sim.runAll(100);
        assertTrue(sim.isWaitingForInput());
        // Only leaq + leaq + call(pause) ran — 3 results
        assertEquals(3, results.size());
    }

    @Test
    void simScanfAndPrintfTogether() {
        sim.load("""
                .rodata
                scanFmt:  .asciz "%d"
                printFmt: .asciz "Got: %d\\n"
                .bss
                val: .skip 4
                .text
                .globl main
                main:
                    leaq scanFmt(%rip), %rdi
                    leaq val(%rip), %rsi
                    call scanf
                    movl val(%rip), %esi
                    leaq printFmt(%rip), %rdi
                    call printf
                    ret
                """);

        sim.step(); // leaq scanFmt
        sim.step(); // leaq val
        sim.step(); // call scanf — pause

        sim.provideInput("55");
        sim.step(); // call scanf — complete

        sim.step(); // movl
        sim.step(); // leaq printFmt

        StepResult printResult = sim.step(); // call printf
        assertEquals("Got: 55\n", printResult.output());
    }
}