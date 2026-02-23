package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.*;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.*;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Disclaimer: Some of the following tests were written by LLMs

class ArithmeticInstructionTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();
    }

    // ==================== ADD/SUB Tests ====================

    @Test
    void addRegistersTest() {
        state.setRegister("rax", 8, 10L);
        state.setRegister("rbx", 8, 20L);
        Instruction add = new AddInstruction("addq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        ));
        add.execute(state, labelManager);
        assertEquals(30L, state.getRegister("rbx", 8));
    }

    @Test
    void subRegistersTest() {
        state.setRegister("rax", 8, 10L);
        state.setRegister("rbx", 8, 20L);
        Instruction sub = new SubInstruction("subq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        ));
        sub.execute(state, labelManager);
        assertEquals(10L, state.getRegister("rbx", 8));
    }

    @Test
    void addMemoryToRegisterTest() {
        state.setRegister("rax", 8, 10L);
        state.getMemory().writeQuad(0x5000L, 20L);
        Instruction add = new AddInstruction("addq", Size.QUAD, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%rax")
        ));
        add.execute(state, labelManager);
        assertEquals(30L, state.getRegister("rax", 8));
    }

    @Test
    void addRegisterToMemoryTest() {
        state.getMemory().writeQuad(0x5000L, 10L);
        state.setRegister("rax", 8, 20L);
        Instruction add = new AddInstruction("addq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        ));
        add.execute(state, labelManager);
        assertEquals(30L, state.getMemory().readQuad(0x5000L));
    }

    @Test
    void addDescriptionTest() {
        state.getMemory().writeQuad(0x5000L, 10L);
        state.setRegister("rax", 8, 20L);
        Instruction add = new AddInstruction("addq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        ));
        assertTrue(add.getDescription(state, labelManager).contains("Added"));
    }

    @Test
    void subDescriptionTest() {
        state.getMemory().writeQuad(0x5000L, 10L);
        state.setRegister("rax", 8, 20L);
        Instruction sub = new SubInstruction("subq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        ));
        assertTrue(sub.getDescription(state, labelManager).contains("Subtracted"));
    }

    // ==================== INC/DEC Tests =========================

    @Test
    void incRegisterTest() {
        state.setRegister("rax", 8, 10L);
        Instruction inc = new IncInstruction("incq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        inc.execute(state, labelManager);
        assertEquals(11L, state.getRegister("rax", 8));
    }

    @Test
    void decRegisterTest() {
        state.setRegister("rax", 8, 10L);
        Instruction dec = new DecInstruction("decq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        dec.execute(state, labelManager);
        assertEquals(9L, state.getRegister("rax", 8));
    }

    @Test
    void incDescriptionTest() {
        state.setRegister("rax", 8, 10L);
        Instruction inc = new IncInstruction("incq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        assertTrue(inc.getDescription(state, labelManager).contains("Incremented"));
    }

    @Test
    void decDescriptionTest() {
        state.setRegister("rax", 8, 10L);
        Instruction dec = new DecInstruction("decq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        assertTrue(dec.getDescription(state, labelManager).contains("Decremented"));
    }

    // ==================== MUL Tests ====================

    @Test
    void mulByteTest() {
        state.setRegister("rax", 1, 10L);
        state.setRegister("rcx", 1, 20L);
        Instruction mul = new MulInstruction("mulb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        ));
        mul.execute(state, labelManager);
        assertEquals(200L, state.getRegister("rax", 2));
    }

    @Test
    void mulByteMaxValuesTest() {
        // 255 * 255 = 65025 (0xFE01) — result spills into AH
        state.setRegister("rax", 1, 0xFFL);
        state.setRegister("rcx", 1, 0xFFL);
        new MulInstruction("mulb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        assertEquals(0xFE01L, state.getRegister("rax", 2));
    }

    @Test
    void mulWordTest() {
        // AX=1000, src=500 → DX:AX = 500000
        state.setRegister("rax", 2, 1000L);
        state.setRegister("rcx", 2, 500L);
        new MulInstruction("mulw", Size.WORD, List.of(
                new RegisterOperand("%cx")
        )).execute(state, labelManager);
        long result = (state.getRegister("rdx", 2) << 16) | state.getRegister("rax", 2);
        assertEquals(500000L, result);
    }

    @Test
    void mulQuadOverflowIntoRdxTest() {
        // RAX=2^63, src=2 → 128-bit result: RAX=0, RDX=1
        state.setRegister("rax", 8, 0x8000000000000000L);
        state.setRegister("rcx", 8, 2L);
        new MulInstruction("mulq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(0L, state.getRegister("rax", 8));
        assertEquals(1L, state.getRegister("rdx", 8));
    }

    @Test
    void mulFromMemoryTest() {
        state.setRegister("rax", 8, 5L);
        state.getMemory().writeQuad(0x5000L, 9L);
        new MulInstruction("mulq", Size.QUAD, List.of(
                new MemoryOperand("0x5000")
        )).execute(state, labelManager);
        assertEquals(45L, state.getRegister("rax", 8));
    }

    @Test
    void mulRejectsImmediateOperandTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new MulInstruction("mulq", Size.QUAD, List.of(
                        new ImmediateOperand("$5")
                ))
        );
    }

    // ==================== DIV Tests ====================

    @Test
    void divByteQuotientAndRemainderTest() {
        // AX=100, divisor=7 → AL=14, AH=2
        state.setRegister("rax", 2, 100L);
        state.setRegister("rcx", 1, 7L);
        new DivInstruction("divb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        long ax = state.getRegister("rax", 2);
        assertEquals(14L, ax & 0xFF);
        assertEquals(2L, (ax >> 8) & 0xFF);
    }

    @Test
    void divWordWithHighPartTest() {
        // DX=1, AX=0 → dividend=65536, divisor=256 → quotient=256, remainder=0
        state.setRegister("rdx", 2, 1L);
        state.setRegister("rax", 2, 0L);
        state.setRegister("rcx", 2, 256L);
        new DivInstruction("divw", Size.WORD, List.of(
                new RegisterOperand("%cx")
        )).execute(state, labelManager);
        assertEquals(256L, state.getRegister("rax", 2));
        assertEquals(0L, state.getRegister("rdx", 2));
    }

    @Test
    void divQuadTest() {
        state.setRegister("rdx", 8, 0L);
        state.setRegister("rax", 8, 100L);
        state.setRegister("rcx", 8, 7L);
        new DivInstruction("divq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(14L, state.getRegister("rax", 8));
        assertEquals(2L, state.getRegister("rdx", 8));
    }

    @Test
    void divByZeroTest() {
        state.setRegister("rax", 8, 100L);
        state.setRegister("rdx", 8, 0L);
        state.setRegister("rcx", 8, 0L);
        Instruction div = new DivInstruction("divq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        ));
        assertThrows(ArithmeticException.class, () -> div.execute(state, labelManager));
    }

    @Test
    void divByteOverflowTest() {
        // AX=0xFFFF, divisor=1 → quotient=65535, doesn't fit in AL
        state.setRegister("rax", 2, 0xFFFFL);
        state.setRegister("rcx", 1, 1L);
        Instruction div = new DivInstruction("divb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        ));
        assertThrows(ArithmeticException.class, () -> div.execute(state, labelManager));
    }

    @Test
    void divQuadOverflowTest() {
        // RDX=1, RAX=0, divisor=1 → quotient=2^64, doesn't fit
        state.setRegister("rdx", 8, 1L);
        state.setRegister("rax", 8, 0L);
        state.setRegister("rcx", 8, 1L);
        Instruction div = new DivInstruction("divq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        ));
        assertThrows(ArithmeticException.class, () -> div.execute(state, labelManager));
    }

    @Test
    void divRejectsImmediateOperandTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new DivInstruction("divq", Size.QUAD, List.of(
                        new ImmediateOperand("$5")
                ))
        );
    }

    // ==================== IMUL Tests ====================

    @Test
    void imulOneOperandByteNegativeTest() {
        // AL=-3 (0xFD), src=-4 (0xFC) → 12
        state.setRegister("rax", 1, 0xFDL);
        state.setRegister("rcx", 1, 0xFCL);
        new IMulInstruction("imulb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        assertEquals(12L, state.getRegister("rax", 2));
    }

    @Test
    void imulOneOperandByteMixedSignTest() {
        // AL=-1 (0xFF), src=127 → -127 (0xFF81 as unsigned 16-bit)
        state.setRegister("rax", 1, 0xFFL);
        state.setRegister("rcx", 1, 0x7FL);
        new IMulInstruction("imulb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        assertEquals(0xFF81L, state.getRegister("rax", 2));
    }

    @Test
    void imulOneOperandLongNegativeTest() {
        // EAX=-2 (0xFFFFFFFE), src=3 → -6 across EDX:EAX
        state.setRegister("rax", 4, 0xFFFFFFFEL);
        state.setRegister("rcx", 4, 3L);
        new IMulInstruction("imull", Size.LONG, List.of(
                new RegisterOperand("%ecx")
        )).execute(state, labelManager);
        assertEquals(0xFFFFFFFAL, state.getRegister("rax", 4));
        assertEquals(0xFFFFFFFFL, state.getRegister("rdx", 4));
    }

    @Test
    void imulOneOperandQuadNegativeTest() {
        // RAX=-1, src=2 → RDX:RAX = -2 (RDX=-1 sign extension)
        state.setRegister("rax", 8, -1L);
        state.setRegister("rcx", 8, 2L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(-2L, state.getRegister("rax", 8));
        assertEquals(-1L, state.getRegister("rdx", 8));
    }

    @Test
    void imulOneOperandRejectsImmediateTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new IMulInstruction("imulq", Size.QUAD, List.of(
                        new ImmediateOperand("$5")
                ))
        );
    }

    @Test
    void imulTwoOperandTest() {
        state.setRegister("rcx", 8, 7L);
        state.setRegister("rbx", 8, 6L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new RegisterOperand("%rcx"),
                new RegisterOperand("%rbx")
        )).execute(state, labelManager);
        assertEquals(42L, state.getRegister("rbx", 8));
    }

    @Test
    void imulTwoOperandNegativeTest() {
        state.setRegister("rcx", 8, -3L);
        state.setRegister("rbx", 8, 4L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new RegisterOperand("%rcx"),
                new RegisterOperand("%rbx")
        )).execute(state, labelManager);
        assertEquals(-12L, state.getRegister("rbx", 8));
    }

    @Test
    void imulTwoOperandTruncationTest() {
        // 32-bit: 0x10000 * 0x10000 = 0x100000000, truncated to 0
        state.setRegister("rcx", 4, 0x10000L);
        state.setRegister("rax", 4, 0x10000L);
        new IMulInstruction("imull", Size.LONG, List.of(
                new RegisterOperand("%ecx"),
                new RegisterOperand("%eax")
        )).execute(state, labelManager);
        assertEquals(0L, state.getRegister("rax", 4));
    }

    @Test
    void imulTwoOperandRejectsByteSizeTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new IMulInstruction("imulb", Size.BYTE, List.of(
                        new RegisterOperand("%al"),
                        new RegisterOperand("%bl")
                ))
        );
    }

    @Test
    void imulThreeOperandTest() {
        state.setRegister("rbx", 8, 6L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new ImmediateOperand("$7"),
                new RegisterOperand("%rbx"),
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(42L, state.getRegister("rcx", 8));
    }

    @Test
    void imulThreeOperandNegativeImmTest() {
        state.setRegister("rbx", 8, 10L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new ImmediateOperand("$-3"),
                new RegisterOperand("%rbx"),
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(-30L, state.getRegister("rcx", 8));
    }

    @Test
    void imulThreeOperandRejectsNonImmFirstTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new IMulInstruction("imulq", Size.QUAD, List.of(
                        new RegisterOperand("%rax"),
                        new RegisterOperand("%rbx"),
                        new RegisterOperand("%rcx")
                ))
        );
    }

    // ==================== IDIV Tests ====================

    @Test
    void idivByteNegativeDividendTest() {
        // AX=-100 (0xFF9C), divisor=7 → quotient=-14, remainder=-2
        state.setRegister("rax", 2, 0xFF9CL);
        state.setRegister("rcx", 1, 7L);
        new IDivInstruction("idivb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        long ax = state.getRegister("rax", 2);
        assertEquals(-14, (byte) (ax & 0xFF));
        assertEquals(-2, (byte) ((ax >> 8) & 0xFF));
    }

    @Test
    void idivByteBothNegativeTest() {
        // AX=-100, divisor=-7 → quotient=14, remainder=-2
        state.setRegister("rax", 2, 0xFF9CL);
        state.setRegister("rcx", 1, 0xF9L);
        new IDivInstruction("idivb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        )).execute(state, labelManager);
        long ax = state.getRegister("rax", 2);
        assertEquals(14, (byte) (ax & 0xFF));
        assertEquals(-2, (byte) ((ax >> 8) & 0xFF));
    }

    @Test
    void idivWordNegativeTest() {
        // DX:AX = -100 (0xFFFF:FF9C), divisor=7 → quotient=-14, remainder=-2
        state.setRegister("rdx", 2, 0xFFFFL);
        state.setRegister("rax", 2, 0xFF9CL);
        state.setRegister("rcx", 2, 7L);
        new IDivInstruction("idivw", Size.WORD, List.of(
                new RegisterOperand("%cx")
        )).execute(state, labelManager);
        assertEquals(0xFFF2L, state.getRegister("rax", 2)); // -14
        assertEquals(0xFFFEL, state.getRegister("rdx", 2)); // -2
    }

    @Test
    void idivLongNegativeTest() {
        // EDX:EAX = -100 (0xFFFFFFFF:FFFFFF9C), divisor=7
        state.setRegister("rdx", 4, 0xFFFFFFFFL);
        state.setRegister("rax", 4, 0xFFFFFF9CL);
        state.setRegister("rcx", 4, 7L);
        new IDivInstruction("idivl", Size.LONG, List.of(
                new RegisterOperand("%ecx")
        )).execute(state, labelManager);
        assertEquals(0xFFFFFFF2L, state.getRegister("rax", 4)); // -14
        assertEquals(0xFFFFFFFEL, state.getRegister("rdx", 4)); // -2
    }

    @Test
    void idivQuadNegativeTest() {
        state.setRegister("rdx", 8, -1L);
        state.setRegister("rax", 8, -100L);
        state.setRegister("rcx", 8, 7L);
        new IDivInstruction("idivq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(-14L, state.getRegister("rax", 8));
        assertEquals(-2L, state.getRegister("rdx", 8));
    }

    @Test
    void idivByZeroTest() {
        state.setRegister("rdx", 8, 0L);
        state.setRegister("rax", 8, 100L);
        state.setRegister("rcx", 8, 0L);
        Instruction idiv = new IDivInstruction("idivq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        ));
        assertThrows(ArithmeticException.class, () -> idiv.execute(state, labelManager));
    }

    @Test
    void idivByteOverflowTest() {
        // AX=0x7FFF, divisor=1 → quotient=32767, doesn't fit in signed byte
        state.setRegister("rax", 2, 0x7FFFL);
        state.setRegister("rcx", 1, 1L);
        Instruction idiv = new IDivInstruction("idivb", Size.BYTE, List.of(
                new RegisterOperand("%cl")
        ));
        assertThrows(ArithmeticException.class, () -> idiv.execute(state, labelManager));
    }

    @Test
    void idivRejectsImmediateOperandTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new IDivInstruction("idivq", Size.QUAD, List.of(
                        new ImmediateOperand("$5")
                ))
        );
    }

    // ==================== Round-trip Test ====================

    @Test
    void imulThenIdivRoundTripTest() {
        // -5 * 3 = -15, then -15 / 3 = -5
        state.setRegister("rax", 8, -5L);
        state.setRegister("rcx", 8, 3L);
        new IMulInstruction("imulq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(-15L, state.getRegister("rax", 8));

        new IDivInstruction("idivq", Size.QUAD, List.of(
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);
        assertEquals(-5L, state.getRegister("rax", 8));
        assertEquals(0L, state.getRegister("rdx", 8));
    }
}
