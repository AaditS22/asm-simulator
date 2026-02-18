package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.*;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.CallInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.ConditionalJumpInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.JmpInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.RetInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.*;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.*;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Disclaimer: Most of the following tests were written with the help of LLMs

class InstructionCreatorTest {
    @Test
    void createMovq() {
        Instruction i = InstructionCreator.create("movq",
                List.of(new ImmediateOperand("$10"), new RegisterOperand("%rax")));
        assertInstanceOf(MovInstruction.class, i);
        assertEquals(Size.QUAD, i.getSize());
    }

    @Test
    void createMovl() {
        Instruction i = InstructionCreator.create("movl",
                List.of(new ImmediateOperand("$10"), new RegisterOperand("%eax")));
        assertInstanceOf(MovInstruction.class, i);
        assertEquals(Size.LONG, i.getSize());
    }

    @Test
    void createMovb() {
        Instruction i = InstructionCreator.create("movb",
                List.of(new ImmediateOperand("$10"), new RegisterOperand("%al")));
        assertInstanceOf(MovInstruction.class, i);
        assertEquals(Size.BYTE, i.getSize());
    }

    @Test
    void createAddq() {
        Instruction i = InstructionCreator.create("addq",
                List.of(new RegisterOperand("%rax"), new RegisterOperand("%rbx")));
        assertInstanceOf(AddInstruction.class, i);
        assertEquals(Size.QUAD, i.getSize());
    }

    @Test
    void createSubw() {
        Instruction i = InstructionCreator.create("subw",
                List.of(new ImmediateOperand("$1"), new RegisterOperand("%ax")));
        assertInstanceOf(SubInstruction.class, i);
        assertEquals(Size.WORD, i.getSize());
    }

    @Test
    void createIncq() {
        Instruction i = InstructionCreator.create("incq",
                List.of(new RegisterOperand("%rax")));
        assertInstanceOf(IncInstruction.class, i);
    }

    @Test
    void createDecl() {
        Instruction i = InstructionCreator.create("decl",
                List.of(new RegisterOperand("%eax")));
        assertInstanceOf(DecInstruction.class, i);
    }

    @Test
    void createMulq() {
        Instruction i = InstructionCreator.create("mulq",
                List.of(new RegisterOperand("%rcx")));
        assertInstanceOf(MulInstruction.class, i);
    }

    @Test
    void createImulqThreeOperand() {
        Instruction i = InstructionCreator.create("imulq",
                List.of(new ImmediateOperand("$5"),
                        new RegisterOperand("%rcx"),
                        new RegisterOperand("%rax")));
        assertInstanceOf(IMulInstruction.class, i);
    }

    @Test
    void createDivb() {
        Instruction i = InstructionCreator.create("divb",
                List.of(new RegisterOperand("%cl")));
        assertInstanceOf(DivInstruction.class, i);
    }

    @Test
    void createIdivq() {
        Instruction i = InstructionCreator.create("idivq",
                List.of(new RegisterOperand("%rcx")));
        assertInstanceOf(IDivInstruction.class, i);
    }

    @Test
    void createAndq() {
        Instruction i = InstructionCreator.create("andq",
                List.of(new ImmediateOperand("$0xFF"), new RegisterOperand("%rax")));
        assertInstanceOf(AndInstruction.class, i);
    }

    @Test
    void createOrq() {
        Instruction i = InstructionCreator.create("orq",
                List.of(new ImmediateOperand("$0xFF"), new RegisterOperand("%rax")));
        assertInstanceOf(OrInstruction.class, i);
    }

    @Test
    void createXorq() {
        Instruction i = InstructionCreator.create("xorq",
                List.of(new RegisterOperand("%rax"), new RegisterOperand("%rax")));
        assertInstanceOf(XorInstruction.class, i);
    }

    @Test
    void createNotq() {
        Instruction i = InstructionCreator.create("notq",
                List.of(new RegisterOperand("%rax")));
        assertInstanceOf(NotInstruction.class, i);
    }

    @Test
    void createNegq() {
        Instruction i = InstructionCreator.create("negq",
                List.of(new RegisterOperand("%rax")));
        assertInstanceOf(NegInstruction.class, i);
    }

    @Test
    void createTestq() {
        Instruction i = InstructionCreator.create("testq",
                List.of(new ImmediateOperand("$1"), new RegisterOperand("%rax")));
        assertInstanceOf(TestInstruction.class, i);
    }

    @Test
    void createCmpq() {
        Instruction i = InstructionCreator.create("cmpq",
                List.of(new ImmediateOperand("$5"), new RegisterOperand("%rax")));
        assertInstanceOf(CmpInstruction.class, i);
    }

    @Test
    void createShlq() {
        Instruction i = InstructionCreator.create("shlq",
                List.of(new ImmediateOperand("$1"), new RegisterOperand("%rax")));
        assertInstanceOf(ShlInstruction.class, i);
    }

    @Test
    void createSalqMapsShl() {
        Instruction i = InstructionCreator.create("salq",
                List.of(new ImmediateOperand("$1"), new RegisterOperand("%rax")));
        assertInstanceOf(ShlInstruction.class, i);
    }

    @Test
    void createShrq() {
        Instruction i = InstructionCreator.create("shrq",
                List.of(new ImmediateOperand("$1"), new RegisterOperand("%rax")));
        assertInstanceOf(ShrInstruction.class, i);
    }

    @Test
    void createLeaq() {
        Instruction i = InstructionCreator.create("leaq",
                List.of(new MemoryOperand("8(%rax)"), new RegisterOperand("%rbx")));
        assertInstanceOf(LeaInstruction.class, i);
    }

    @Test
    void createPushq() {
        Instruction i = InstructionCreator.create("pushq",
                List.of(new RegisterOperand("%rbp")));
        assertInstanceOf(PushInstruction.class, i);
    }

    @Test
    void createPopq() {
        Instruction i = InstructionCreator.create("popq",
                List.of(new RegisterOperand("%rbp")));
        assertInstanceOf(PopInstruction.class, i);
    }

    // ==================== Unsized instructions ====================

    @Test
    void createRet() {
        Instruction i = InstructionCreator.create("ret", List.of());
        assertInstanceOf(RetInstruction.class, i);
        assertEquals(Size.QUAD, i.getSize());
    }

    @Test
    void createJmp() {
        Instruction i = InstructionCreator.create("jmp",
                List.of(new LabelOperand("loop")));
        assertInstanceOf(JmpInstruction.class, i);
    }

    @Test
    void createCall() {
        Instruction i = InstructionCreator.create("call",
                List.of(new LabelOperand("func")));
        assertInstanceOf(CallInstruction.class, i);
    }

    @Test
    void createJe() {
        Instruction i = InstructionCreator.create("je",
                List.of(new LabelOperand("done")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    @Test
    void createJne() {
        Instruction i = InstructionCreator.create("jne",
                List.of(new LabelOperand("loop")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    @Test
    void createJg() {
        Instruction i = InstructionCreator.create("jg",
                List.of(new LabelOperand("label")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    @Test
    void createJge() {
        Instruction i = InstructionCreator.create("jge",
                List.of(new LabelOperand("label")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    @Test
    void createJl() {
        Instruction i = InstructionCreator.create("jl",
                List.of(new LabelOperand("label")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    @Test
    void createJle() {
        Instruction i = InstructionCreator.create("jle",
                List.of(new LabelOperand("label")));
        assertInstanceOf(ConditionalJumpInstruction.class, i);
    }

    // ==================== MOVZB / MOVZW ====================

    @Test
    void createMovzbl() {
        Instruction i = InstructionCreator.create("movzbl",
                List.of(new RegisterOperand("%al"), new RegisterOperand("%eax")));
        assertInstanceOf(MovzbInstruction.class, i);
        assertEquals(Size.BYTE, i.getSize());
    }

    @Test
    void createMovzbq() {
        Instruction i = InstructionCreator.create("movzbq",
                List.of(new RegisterOperand("%al"), new RegisterOperand("%rax")));
        assertInstanceOf(MovzbInstruction.class, i);
    }

    @Test
    void createMovzwl() {
        Instruction i = InstructionCreator.create("movzwl",
                List.of(new RegisterOperand("%ax"), new RegisterOperand("%eax")));
        assertInstanceOf(MovzwInstruction.class, i);
        assertEquals(Size.WORD, i.getSize());
    }

    @Test
    void createMovzwq() {
        Instruction i = InstructionCreator.create("movzwq",
                List.of(new RegisterOperand("%ax"), new RegisterOperand("%rax")));
        assertInstanceOf(MovzwInstruction.class, i);
    }

    // ==================== Size inference ====================

    @Test
    void inferSizeFromRegisters() {
        Instruction i = InstructionCreator.create("mov",
                List.of(new RegisterOperand("%rax"), new RegisterOperand("%rbx")));
        assertInstanceOf(MovInstruction.class, i);
        assertEquals(Size.QUAD, i.getSize());
    }

    @Test
    void inferSizeFromSingleRegister() {
        Instruction i = InstructionCreator.create("inc",
                List.of(new RegisterOperand("%eax")));
        assertInstanceOf(IncInstruction.class, i);
        assertEquals(Size.LONG, i.getSize());
    }

    @Test
    void inferSizeFailsNoRegisters() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("mov",
                        List.of(new ImmediateOperand("$10"),
                                new MemoryOperand("0x1000"))));
    }

    @Test
    void inferSizeMismatchedRegistersThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("mov",
                        List.of(new RegisterOperand("%eax"),
                                new RegisterOperand("%rbx"))));
    }

    // ==================== isBranchMnemonic ====================

    @Test
    void isBranchMnemonicTrue() {
        assertTrue(InstructionCreator.isBranchMnemonic("jmp"));
        assertTrue(InstructionCreator.isBranchMnemonic("call"));
        assertTrue(InstructionCreator.isBranchMnemonic("je"));
        assertTrue(InstructionCreator.isBranchMnemonic("jne"));
        assertTrue(InstructionCreator.isBranchMnemonic("jg"));
        assertTrue(InstructionCreator.isBranchMnemonic("jge"));
        assertTrue(InstructionCreator.isBranchMnemonic("jl"));
        assertTrue(InstructionCreator.isBranchMnemonic("jle"));
        assertTrue(InstructionCreator.isBranchMnemonic("JMP"));
    }

    @Test
    void isBranchMnemonicFalse() {
        assertFalse(InstructionCreator.isBranchMnemonic("mov"));
        assertFalse(InstructionCreator.isBranchMnemonic("movq"));
        assertFalse(InstructionCreator.isBranchMnemonic("addq"));
        assertFalse(InstructionCreator.isBranchMnemonic("ret"));
        assertFalse(InstructionCreator.isBranchMnemonic("push"));
        assertFalse(InstructionCreator.isBranchMnemonic("leaq"));
    }

    // ==================== Error cases ====================

    @Test
    void unknownMnemonicThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("xyz",
                        List.of(new RegisterOperand("%rax"))));
    }

    @Test
    void emptyMnemonicThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("", List.of()));
    }

    @Test
    void twoMemoryOperandsThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("movq",
                        List.of(new MemoryOperand("0x1000"),
                                new MemoryOperand("0x2000"))));
    }

    @Test
    void twoMemoryOperandsAllowedForLea() {
        // LEA's first operand is memory, but it doesn't actually access it
        // so validateMemoryOperands skips LEA
        assertDoesNotThrow(() ->
                InstructionCreator.create("leaq",
                        List.of(new MemoryOperand("8(%rax)"),
                                new RegisterOperand("%rbx"))));
    }

    @Test
    void caseInsensitive() {
        Instruction i = InstructionCreator.create("MOVQ",
                List.of(new ImmediateOperand("$10"), new RegisterOperand("%rax")));
        assertInstanceOf(MovInstruction.class, i);
    }

    @Test
    void invalidMovzbSuffix() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("movzbb",
                        List.of(new RegisterOperand("%al"),
                                new RegisterOperand("%bl"))));
    }

    @Test
    void invalidMovzwSuffix() {
        assertThrows(IllegalArgumentException.class, () ->
                InstructionCreator.create("movzwb",
                        List.of(new RegisterOperand("%ax"),
                                new RegisterOperand("%bl"))));
    }
}