package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.*;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.*;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Disclaimer: Some of the following tests were written by LLMs

class LogicalInstructionTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();
    }

    // ==================== AND / OR / XOR ====================

    @Test
    void andRegisterToRegisterUpdatesFlagsAndWritesDestTest() {
        state.setRegister("rax", 1, 0xF0L);
        state.setRegister("rbx", 1, 0x0FL);

        new AndInstruction("andb", Size.BYTE, List.of(
                new RegisterOperand("%al"),
                new RegisterOperand("%bl")
        )).execute(state, labelManager);

        // 0xF0 & 0x0F = 0x00
        assertEquals(0x00L, state.getRegister("rbx", 1));
        assertTrue(state.getFlags().isZero());
        assertFalse(state.getFlags().isNegative());
        assertFalse(state.getFlags().isCarry());
        assertFalse(state.getFlags().isOverflow());
    }

    @Test
    void andRegisterToMemoryTest() {
        state.setRegister("rax", 8, 0xFF00FF00FF00FF00L);
        state.getMemory().writeQuad(0x5000L, 0x0F0F0F0F0F0F0F0FL);

        new AndInstruction("andq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        )).execute(state, labelManager);

        assertEquals(0x0F000F000F000F00L, state.getMemory().readQuad(0x5000L));
        assertFalse(state.getFlags().isZero());
        assertFalse(state.getFlags().isCarry());
        assertFalse(state.getFlags().isOverflow());
    }

    @Test
    void andRejectsTwoMemoryOperandsTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new AndInstruction("andq", Size.QUAD, List.of(
                        new MemoryOperand("0x5000"),
                        new MemoryOperand("0x6000")
                ))
        );
    }

    @Test
    void orSetsSignFlagAndClearsCarryOverflowTest() {
        // 0x80 | 0x01 = 0x81 => sign bit set for BYTE
        state.setRegister("rax", 1, 0x80L);
        state.setRegister("rbx", 1, 0x01L);

        // Make carry/overflow true first, so we can confirm they get cleared
        state.getFlags().updateAddFlags(0x7F, 0x7F, 8);
        assertTrue(state.getFlags().isOverflow() || state.getFlags().isCarry());

        new OrInstruction("orb", Size.BYTE, List.of(
                new RegisterOperand("%al"),
                new RegisterOperand("%bl")
        )).execute(state, labelManager);

        assertEquals(0x81L, state.getRegister("rbx", 1));
        assertFalse(state.getFlags().isZero());
        assertTrue(state.getFlags().isNegative());
        assertFalse(state.getFlags().isCarry());
        assertFalse(state.getFlags().isOverflow());
    }

    @Test
    void orRejectsTwoMemoryOperandsTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new OrInstruction("orq", Size.QUAD, List.of(
                        new MemoryOperand("0x5000"),
                        new MemoryOperand("0x6000")
                ))
        );
    }

    @Test
    void xorRegisterToRegisterProducesZeroTest() {
        state.setRegister("rax", 8, 0xDEADBEEFL);
        state.setRegister("rbx", 8, 0xDEADBEEFL);

        new XorInstruction("xorq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        )).execute(state, labelManager);

        assertEquals(0L, state.getRegister("rbx", 8));
        assertTrue(state.getFlags().isZero());
        assertFalse(state.getFlags().isNegative());
        assertFalse(state.getFlags().isCarry());
        assertFalse(state.getFlags().isOverflow());
    }

    @Test
    void xorRejectsTwoImmediateOperandsTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new XorInstruction("xorq", Size.QUAD, List.of(
                        new ImmediateOperand("$1"),
                        new ImmediateOperand("$2")
                ))
        );
    }

    // ==================== TEST ====================

    @Test
    void testUpdatesFlagsButDoesNotWriteOperandsTest() {
        state.setRegister("rax", 1, 0xF0L);
        state.setRegister("rbx", 1, 0x0FL);

        new TestInstruction("testb", Size.BYTE, List.of(
                new RegisterOperand("%al"),
                new RegisterOperand("%bl")
        )).execute(state, labelManager);

        // Operands should remain unchanged
        assertEquals(0xF0L, state.getRegister("rax", 1));
        assertEquals(0x0FL, state.getRegister("rbx", 1));

        // Flags come from 0xF0 & 0x0F = 0
        assertTrue(state.getFlags().isZero());
        assertFalse(state.getFlags().isNegative());
        assertFalse(state.getFlags().isCarry());
        assertFalse(state.getFlags().isOverflow());
    }

    @Test
    void testRejectsTwoImmediateOperandsTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestInstruction("testq", Size.QUAD, List.of(
                        new ImmediateOperand("$1"),
                        new ImmediateOperand("$2")
                ))
        );
    }

    // ==================== CMP ====================

    @Test
    void cmpEqualSetsZeroFlagWithoutModifyingOperandsTest() {
        state.setRegister("rax", 8, 42L);
        state.setRegister("rbx", 8, 42L);

        new CmpInstruction("cmpq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        )).execute(state, labelManager);

        assertEquals(42L, state.getRegister("rax", 8));
        assertEquals(42L, state.getRegister("rbx", 8));
        assertTrue(state.getFlags().isZero());
        assertFalse(state.getFlags().isNegative());
    }

    @Test
    void cmpUpdatesSignAndCarryWhenBorrowNeededTest() {
        // cmpq $5, %rax with rax=3 => 3-5 = -2
        state.setRegister("rax", 8, 3L);
        new CmpInstruction("cmpq", Size.QUAD, List.of(
                new ImmediateOperand("$5"),
                new RegisterOperand("%rax")
        )).execute(state, labelManager);

        assertFalse(state.getFlags().isZero());
        assertTrue(state.getFlags().isNegative());
        assertTrue(state.getFlags().isCarry());
    }

    @Test
    void cmpRejectsTwoImmediateOperandsTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new CmpInstruction("cmpq", Size.QUAD, List.of(
                        new ImmediateOperand("$1"),
                        new ImmediateOperand("$2")
                ))
        );
    }

    // ==================== NOT / NEG ====================

    @Test
    void notInvertsBitsAndDoesNotChangeFlagsTest() {
        state.setRegister("rax", 1, 0x0FL);

        // Set some flags to non-default values
        state.getFlags().updateAddFlags(0x7F, 1, 8); // result 0x80 -> SF=true, OF=true
        boolean zfBefore = state.getFlags().isZero();
        boolean sfBefore = state.getFlags().isNegative();
        boolean cfBefore = state.getFlags().isCarry();
        boolean ofBefore = state.getFlags().isOverflow();

        new NotInstruction("notb", Size.BYTE, List.of(
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0xF0L, state.getRegister("rax", 1));
        assertEquals(zfBefore, state.getFlags().isZero());
        assertEquals(sfBefore, state.getFlags().isNegative());
        assertEquals(cfBefore, state.getFlags().isCarry());
        assertEquals(ofBefore, state.getFlags().isOverflow());
    }

    @Test
    void notRejectsImmediateOperandTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new NotInstruction("notq", Size.QUAD, List.of(
                        new ImmediateOperand("$1")
                ))
        );
    }

    @Test
    void negZeroClearsCarryAndSetsZeroFlagTest() {
        state.setRegister("rax", 1, 0x00L);

        new NegInstruction("negb", Size.BYTE, List.of(
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0x00L, state.getRegister("rax", 1));
        assertTrue(state.getFlags().isZero());
        assertFalse(state.getFlags().isCarry());
    }

    @Test
    void negNonZeroSetsCarryTest() {
        state.setRegister("rax", 1, 0x01L);

        new NegInstruction("negb", Size.BYTE, List.of(
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0xFFL, state.getRegister("rax", 1));
        assertFalse(state.getFlags().isZero());
        assertTrue(state.getFlags().isCarry());
        assertTrue(state.getFlags().isNegative());
    }

    @Test
    void negMostNegativeByteSetsOverflowTest() {
        // In 8-bit, -128 negated overflows (result is still -128)
        state.setRegister("rax", 1, 0x80L);

        new NegInstruction("negb", Size.BYTE, List.of(
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0x80L, state.getRegister("rax", 1));
        assertTrue(state.getFlags().isOverflow());
        assertTrue(state.getFlags().isCarry());
        assertTrue(state.getFlags().isNegative());
    }

    @Test
    void negRejectsImmediateOperandTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new NegInstruction("negq", Size.QUAD, List.of(
                        new ImmediateOperand("$1")
                ))
        );
    }

    // ==================== SHL / SHR ====================

    @Test
    void shlByOneUpdatesCarryAndOverflowTest() {
        // 0x7F << 1 = 0xFE; CF becomes old bit7 (0), OF = sign(result) XOR sign(operand) => true
        state.setRegister("rax", 1, 0x7FL);

        new ShlInstruction("shlb", Size.BYTE, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0xFEL, state.getRegister("rax", 1));
        assertFalse(state.getFlags().isCarry());
        assertTrue(state.getFlags().isOverflow());
        assertTrue(state.getFlags().isNegative());
        assertFalse(state.getFlags().isZero());
    }

    @Test
    void shlMasksShiftCountForQuadTest() {
        // 65 masked with 0x3F => 1
        state.setRegister("rax", 8, 1L);

        new ShlInstruction("shlq", Size.QUAD, List.of(
                new ImmediateOperand("$65"),
                new RegisterOperand("%rax")
        )).execute(state, labelManager);

        assertEquals(2L, state.getRegister("rax", 8));
    }

    @Test
    void shlCountZeroDoesNotChangeFlagsTest() {
        // Set flags to a known non-default state
        state.getFlags().updateAddFlags(0x7F, 1, 8); // SF=true, OF=true
        boolean zfBefore = state.getFlags().isZero();
        boolean sfBefore = state.getFlags().isNegative();
        boolean cfBefore = state.getFlags().isCarry();
        boolean ofBefore = state.getFlags().isOverflow();

        state.setRegister("rax", 1, 0x12L);
        new ShlInstruction("shlb", Size.BYTE, List.of(
                new ImmediateOperand("$0"),
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0x12L, state.getRegister("rax", 1));
        assertEquals(zfBefore, state.getFlags().isZero());
        assertEquals(sfBefore, state.getFlags().isNegative());
        assertEquals(cfBefore, state.getFlags().isCarry());
        assertEquals(ofBefore, state.getFlags().isOverflow());
    }

    @Test
    void shrByOneUpdatesCarryAndOverflowTest() {
        // 0x80 >>> 1 = 0x40; CF becomes old bit0 (0), OF becomes original sign bit (1)
        state.setRegister("rax", 1, 0x80L);

        new ShrInstruction("shrb", Size.BYTE, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%al")
        )).execute(state, labelManager);

        assertEquals(0x40L, state.getRegister("rax", 1));
        assertFalse(state.getFlags().isCarry());
        assertTrue(state.getFlags().isOverflow());
        assertFalse(state.getFlags().isNegative());
        assertFalse(state.getFlags().isZero());
    }

    @Test
    void shrMasksShiftCountForLongTest() {
        // For 32-bit, count masked with 0x1F => 1
        state.setRegister("rax", 4, 4L);

        new ShrInstruction("shrl", Size.LONG, List.of(
                new ImmediateOperand("$33"),
                new RegisterOperand("%eax")
        )).execute(state, labelManager);

        assertEquals(2L, state.getRegister("rax", 4));
    }

    // ==================== Description Tests ====================

    @Test
    void descriptionsContainMnemonicIntentTest() {
        state.setRegister("rax", 8, 1L);
        Instruction and = new AndInstruction("andq", Size.QUAD, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%rax")
        ));
        Instruction cmp = new CmpInstruction("cmpq", Size.QUAD, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%rax")
        ));
        Instruction shl = new ShlInstruction("shlq", Size.QUAD, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%rax")
        ));

        assertTrue(and.getDescription(state, labelManager).toLowerCase().contains("and"));
        assertTrue(cmp.getDescription(state, labelManager).toLowerCase().contains("subtract"));
        assertTrue(shl.getDescription(state, labelManager).toLowerCase().contains("shift"));
    }

    @Test
    void nopAdvancesPc() {
        long pcBefore = state.getPC();
        new NopInstruction("nop", Size.QUAD, List.of()).execute(state, labelManager);
        assertEquals(pcBefore + MemoryLayout.INSTRUCTION_SIZE, state.getPC());
    }

    @Test
    void nopDoesNotModifyRegisters() {
        state.setRegister("rax", 8, 0xDEADBEEFL);
        state.setRegister("rbx", 8, 0xCAFEBABEL);
        new NopInstruction("nop", Size.QUAD, List.of()).execute(state, labelManager);
        assertEquals(0xDEADBEEFL, state.getRegister("rax", 8));
        assertEquals(0xCAFEBABEL, state.getRegister("rbx", 8));
    }

    @Test
    void nopDoesNotModifyFlags() {
        state.getFlags().updateAddFlags(0x7FFFFFFFFFFFFFFFL, 1L, 64);
        boolean zf = state.getFlags().isZero();
        boolean sf = state.getFlags().isNegative();
        boolean cf = state.getFlags().isCarry();
        boolean of = state.getFlags().isOverflow();

        new NopInstruction("nop", Size.QUAD, List.of()).execute(state, labelManager);

        assertEquals(zf, state.getFlags().isZero());
        assertEquals(sf, state.getFlags().isNegative());
        assertEquals(cf, state.getFlags().isCarry());
        assertEquals(of, state.getFlags().isOverflow());
    }

    @Test
    void nopRejectsOperands() {
        assertThrows(IllegalArgumentException.class, () ->
                new NopInstruction("nop", Size.QUAD, List.of(new RegisterOperand("%rax"))));
    }

    @Test
    void nopDescriptionIsNonEmpty() {
        String desc = new NopInstruction("nop", Size.QUAD, List.of())
                .getDescription(state, labelManager);
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }
}
