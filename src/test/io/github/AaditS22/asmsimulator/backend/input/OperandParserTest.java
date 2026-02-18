package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// DISCLAIMER: The following tests were written with the help of LLMs

class OperandParserTest {
    @Test
    void parseImmediateDecimal() {
        Operand op = OperandParser.parse("$100", false);
        assertInstanceOf(ImmediateOperand.class, op);
    }

    @Test
    void parseImmediateHex() {
        Operand op = OperandParser.parse("$0xFF", false);
        assertInstanceOf(ImmediateOperand.class, op);
    }

    @Test
    void parseImmediateLabel() {
        Operand op = OperandParser.parse("$my_var", false);
        assertInstanceOf(ImmediateOperand.class, op);
    }

    @Test
    void parseImmediateNegative() {
        Operand op = OperandParser.parse("$-42", false);
        assertInstanceOf(ImmediateOperand.class, op);
    }

    @Test
    void parseRegister() {
        Operand op = OperandParser.parse("%rax", false);
        assertInstanceOf(RegisterOperand.class, op);
    }

    @Test
    void parseRegisterSubRegister() {
        Operand op = OperandParser.parse("%al", false);
        assertInstanceOf(RegisterOperand.class, op);
    }

    @Test
    void parseMemoryBaseOnly() {
        Operand op = OperandParser.parse("(%rax)", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseMemoryDisplacementBase() {
        Operand op = OperandParser.parse("16(%rax)", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseMemoryBaseIndexScale() {
        Operand op = OperandParser.parse("8(%rax,%rcx,4)", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseMemoryLabelDisplacement() {
        Operand op = OperandParser.parse("my_var(%rip)", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseNumericAbsoluteAddress() {
        Operand op = OperandParser.parse("0x601000", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseNumericDecimalAddress() {
        Operand op = OperandParser.parse("1000", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void parseLabelOperand() {
        Operand op = OperandParser.parse("loop_start", true);
        assertInstanceOf(LabelOperand.class, op);
    }

    @Test
    void parseLabelWithUnderscore() {
        Operand op = OperandParser.parse("_start", true);
        assertInstanceOf(LabelOperand.class, op);
    }

    @Test
    void bareLabelDefaultsToMemoryOperand() {
        Operand op = OperandParser.parse("counter", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void bareLabelWithFalseIsMemoryOperand() {
        Operand op = OperandParser.parse("my_var", false);
        assertInstanceOf(MemoryOperand.class, op);
    }

    @Test
    void bareLabelWithTrueIsLabelOperand() {
        Operand op = OperandParser.parse("my_var", true);
        assertInstanceOf(LabelOperand.class, op);
    }

    @Test
    void parseEmptyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> OperandParser.parse("", false));
    }

    @Test
    void parseAllSimpleTwoOperands() {
        List<Operand> ops = OperandParser.parseAll("$10, %rax", false);
        assertEquals(2, ops.size());
        assertInstanceOf(ImmediateOperand.class, ops.get(0));
        assertInstanceOf(RegisterOperand.class, ops.get(1));
    }

    @Test
    void parseAllMemoryWithCommasInside() {
        List<Operand> ops = OperandParser.parseAll("8(%rax,%rcx,4), %rbx", false);
        assertEquals(2, ops.size());
        assertInstanceOf(MemoryOperand.class, ops.get(0));
        assertInstanceOf(RegisterOperand.class, ops.get(1));
    }

    @Test
    void parseAllSingleOperand() {
        List<Operand> ops = OperandParser.parseAll("%rax", false);
        assertEquals(1, ops.size());
        assertInstanceOf(RegisterOperand.class, ops.get(0));
    }

    @Test
    void parseAllEmpty() {
        List<Operand> ops = OperandParser.parseAll("", false);
        assertTrue(ops.isEmpty());
    }

    @Test
    void parseAllNull() {
        List<Operand> ops = OperandParser.parseAll(null, false);
        assertTrue(ops.isEmpty());
    }

    @Test
    void parseAllThreeOperandsImul() {
        List<Operand> ops = OperandParser.parseAll("$5, %rcx, %rax", false);
        assertEquals(3, ops.size());
        assertInstanceOf(ImmediateOperand.class, ops.get(0));
        assertInstanceOf(RegisterOperand.class, ops.get(1));
        assertInstanceOf(RegisterOperand.class, ops.get(2));
    }

    @Test
    void parseAllBranchTargetLabel() {
        List<Operand> ops = OperandParser.parseAll("loop_start", true);
        assertEquals(1, ops.size());
        assertInstanceOf(LabelOperand.class, ops.get(0));
    }

    @Test
    void parseAllNonBranchBareLabel() {
        List<Operand> ops = OperandParser.parseAll("counter, %rax", false);
        assertEquals(2, ops.size());
        assertInstanceOf(MemoryOperand.class, ops.get(0));
        assertInstanceOf(RegisterOperand.class, ops.get(1));
    }

    @Test
    void parseAllTrailingCommaThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> OperandParser.parseAll("$10,", false));
    }

    @Test
    void parseAllEmptyBetweenCommasThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> OperandParser.parseAll("$10, , %rax", false));
    }

    @Test
    void splitRespectsParenthesesDepth() {
        List<String> result = OperandParser.splitOperands(
                "8(%rax,%rcx,4), %rbx");
        assertEquals(2, result.size());
        assertEquals("8(%rax,%rcx,4)", result.get(0));
        assertEquals("%rbx", result.get(1));
    }

    @Test
    void isLabelOperandTests() {
        assertTrue(OperandParser.isLabelOperand("loop"));
        assertTrue(OperandParser.isLabelOperand("_start"));
        assertFalse(OperandParser.isLabelOperand("$100"));
        assertFalse(OperandParser.isLabelOperand("%rax"));
        assertFalse(OperandParser.isLabelOperand("(%rax)"));
        assertFalse(OperandParser.isLabelOperand("0x100"));
        assertFalse(OperandParser.isLabelOperand("42"));
    }

    @Test
    void parseWhitespaceHandling() {
        Operand op = OperandParser.parse("  %rax  ", false);
        assertInstanceOf(RegisterOperand.class, op);

        List<Operand> ops = OperandParser.parseAll("  $10 ,  %rax  ", false);
        assertEquals(2, ops.size());
    }
}