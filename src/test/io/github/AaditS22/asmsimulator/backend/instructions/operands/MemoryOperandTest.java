package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// DISCLAIMER: The following tests were written by an LLM

class MemoryOperandTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    public void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();

        // Set up some test registers
        state.setRegister("rax", 8, 0x1000);
        state.setRegister("rbx", 8, 0x2000);
        state.setRegister("rcx", 8, 0x10);

        // Set up some test memory values
        state.getMemory().writeQuad(0x1000, 0x1111111111111111L);
        state.getMemory().writeQuad(0x2000, 0x2222222222222222L);
        state.getMemory().writeQuad(0x1010, 0x3333333333333333L);
        state.getMemory().writeQuad(0x1020, 0x4444444444444444L);

        // Set up test labels
        labelManager.addDataLabel("my_var", 0x3000, 8);
        labelManager.addDataLabel("array_start", 0x4000, 8);
        state.getMemory().writeQuad(0x3000, 0x5555555555555555L);
        state.getMemory().writeQuad(0x4000, 0xAAAAAAAAAAAAAAAAL);
        state.getMemory().writeQuad(0x4008, 0xBBBBBBBBBBBBBBBBL);
        state.getMemory().writeQuad(3 + MemoryLayout.CODE_BASE, 0x9999999999999999L);
    }

    @Test
    public void testSimpleNumericDisplacement() {
        // Test: movq 0x1000, %rax
        MemoryOperand operand = new MemoryOperand("0x1000");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x1111111111111111L, value);
    }

    @Test
    public void testSimpleLabelDisplacement() {
        // Test: movq my_var, %rax
        MemoryOperand operand = new MemoryOperand("my_var");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x5555555555555555L, value);
    }

    @Test
    public void testBaseRegisterOnly() {
        // Test: movq (%rax), %rbx  where %rax = 0x1000
        MemoryOperand operand = new MemoryOperand("(%rax)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x1111111111111111L, value);
    }

    @Test
    public void testDisplacementPlusBase() {
        // Test: movq 16(%rax), %rbx  where %rax = 0x1000
        // Should access 0x1000 + 16 = 0x1010
        MemoryOperand operand = new MemoryOperand("16(%rax)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x3333333333333333L, value);
    }

    @Test
    public void testNegativeDisplacement() {
        // Test: movq -16(%rbx), %rax  where %rbx = 0x2000
        // Should access 0x2000 - 16 = 0x1FF0
        state.getMemory().writeQuad(0x1FF0, 0x9999999999999999L);
        MemoryOperand operand = new MemoryOperand("-16(%rbx)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x9999999999999999L, value);
    }

    @Test
    public void testLabelAsDisplacement() {
        // Test: movq my_var(%rax), %rbx  where my_var is at 0x3000, %rax = 0x1000
        // Should access 0x3000 + 0x1000 = 0x4000
        MemoryOperand operand = new MemoryOperand("my_var(%rax)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0xAAAAAAAAAAAAAAAAL, value);
    }

    @Test
    public void testBaseAndIndex() {
        // Test: movq (%rax,%rcx), %rbx  where %rax = 0x1000, %rcx = 0x10
        // Should access 0x1000 + 0x10 = 0x1010
        MemoryOperand operand = new MemoryOperand("(%rax,%rcx)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x3333333333333333L, value);
    }

    @Test
    public void testBaseIndexScale() {
        // Test: movq (%rax,%rcx,2), %rbx  where %rax = 0x1000, %rcx = 0x10
        // Should access 0x1000 + (0x10 * 2) = 0x1020
        MemoryOperand operand = new MemoryOperand("(%rax,%rcx,2)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x4444444444444444L, value);
    }

    @Test
    public void testDisplacementBaseIndexScale() {
        // Test: movq 8(%rax,%rcx,2), %rbx  where %rax = 0x1000, %rcx = 0x10
        // Should access 0x1000 + 8 + (0x10 * 2) = 0x1028
        state.getMemory().writeQuad(0x1028, 0x7777777777777777L);
        MemoryOperand operand = new MemoryOperand("8(%rax,%rcx,2)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x7777777777777777L, value);
    }

    @Test
    public void testArrayAccess() {
        // Test: movq array_start(,%rcx,8), %rax  where %rcx = 1
        // Simulates: array_start[1] where each element is 8 bytes
        // Should access 0x4000 + (1 * 8) = 0x4008
        state.setRegister("rcx", 8, 1);
        MemoryOperand operand = new MemoryOperand("array_start(,%rcx,8)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0xBBBBBBBBBBBBBBBBL, value);
    }

    // ========== Scale Validation Tests ==========

    @Test
    public void testValidScales() {
        // All valid scales should work
        int[] validScales = {1, 2, 4, 8};
        for (int scale : validScales) {
            assertDoesNotThrow(() -> {
                MemoryOperand op = new MemoryOperand("(%rax,%rcx," + scale + ")");
                op.getValue(state, labelManager, Size.QUAD);
            });
        }
    }

    @Test
    public void testInvalidScale() {
        // Invalid scale should throw an exception
        MemoryOperand operand = new MemoryOperand("(%rax,%rcx,3)");
        assertThrows(IllegalArgumentException.class, () -> {
            operand.getValue(state, labelManager, Size.QUAD);
        });
    }

    @Test
    public void testEmptyBase() {
        // Test: movq 0x1000(,%rcx,2), %rax  where %rcx = 8
        // Should access 0x1000 + (8 * 2) = 0x1010
        state.setRegister("rcx", 8, 8);
        MemoryOperand operand = new MemoryOperand("0x1000(,%rcx,2)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x3333333333333333L, value);
    }

    @Test
    public void testEmptyDisplacement() {
        // Test: movq (%rax), %rbx
        MemoryOperand operand = new MemoryOperand("(%rax)");
        long value = operand.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x1111111111111111L, value);
    }

    @Test
    public void testInvalidLabel() {
        // Test with a non-existent label
        MemoryOperand operand = new MemoryOperand("nonexistent_label");
        assertThrows(IllegalArgumentException.class, () -> {
            operand.getValue(state, labelManager, Size.QUAD);
        });
    }

    @Test
    public void testMismatchedParentheses() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MemoryOperand("8(%rax");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new MemoryOperand("8%rax)");
        });
    }

    @Test
    public void testTooManyArguments() {
        // More than 3 components in parentheses
        MemoryOperand operand = new MemoryOperand("(%rax,%rbx,%rcx,2)");
        assertThrows(IllegalArgumentException.class, () -> {
            operand.getValue(state, labelManager, Size.QUAD);
        });
    }

    @Test
    public void testSetValueSimple() {
        // Test: movq %rax, 0x5000
        MemoryOperand operand = new MemoryOperand("0x5000");
        operand.setValue(state, labelManager, 0xDEADBEEFCAFEBABEL, Size.QUAD);

        long value = state.getMemory().readQuad(0x5000);
        assertEquals(0xDEADBEEFCAFEBABEL, value);
    }

    @Test
    public void testSetValueComplex() {
        // Test: movq %rax, 8(%rbx,%rcx,4)  where %rbx = 0x2000, %rcx = 2
        // Should write to 0x2000 + 8 + (2 * 4) = 0x2010
        state.setRegister("rcx", 8, 2);
        MemoryOperand operand = new MemoryOperand("8(%rbx,%rcx,4)");
        operand.setValue(state, labelManager, 0x8888888888888888L, Size.QUAD);

        long value = state.getMemory().readQuad(0x2010);
        assertEquals(0x8888888888888888L, value);
    }

    @Test
    public void testDifferentSizes() {
        state.getMemory().writeByte(0x6000, (byte) 0x42);
        state.getMemory().writeWord(0x6001, (short) 0x1234);
        state.getMemory().writeLong(0x6003, 0x12345678);

        MemoryOperand byteOp = new MemoryOperand("0x6000");
        assertEquals(0x42L, byteOp.getValue(state, labelManager, Size.BYTE));

        MemoryOperand wordOp = new MemoryOperand("0x6001");
        assertEquals(0x1234L, wordOp.getValue(state, labelManager, Size.WORD));

        MemoryOperand longOp = new MemoryOperand("0x6003");
        assertEquals(0x12345678L, longOp.getValue(state, labelManager, Size.LONG));
    }

    @Test
    public void testRIPAddressing() {
        MemoryOperand operand = new MemoryOperand("my_var(%rip)");
        assertEquals(0x5555555555555555L, operand.getValue(state, labelManager, Size.QUAD));
        MemoryOperand operand2 = new MemoryOperand("3(%rip)");
        assertEquals(0x9999999999999999L, operand2.getValue(state, labelManager, Size.QUAD));
    }

    @Test
    public void testRIPExceptions() {
        MemoryOperand operand = new MemoryOperand("my_var(%rip, %abc)");
        assertThrows(IllegalArgumentException.class, () -> {
            operand.getValue(state, labelManager, Size.QUAD);
        });
        MemoryOperand operand2 = new MemoryOperand("4(%rax, %rip, 2)");
        assertThrows(IllegalArgumentException.class, () -> {
            operand2.getValue(state, labelManager, Size.QUAD);
        });
    }

    @Test
    public void testToAssemblyString() {
        assertEquals("0x1000", new MemoryOperand("0x1000").toAssemblyString());
        assertEquals("8(%rax,%rcx,4)", new MemoryOperand("8(%rax,%rcx,4)").toAssemblyString());
    }

    @Test
    public void testGetDescriptionOnlyDisplacement() {
        // Numeric displacement
        MemoryOperand op1 = new MemoryOperand("0x1000");
        assertEquals("memory at effective address 0x1000 (resolved as: displacement 0x1000)",
                op1.getDescription(state, labelManager));

        // Label displacement
        MemoryOperand op2 = new MemoryOperand("my_var");
        assertEquals("memory at effective address 0x3000 (resolved as: address of label my_var)",
                op2.getDescription(state, labelManager));
    }

    @Test
    public void testGetDescriptionComplexAddressing() {
        // Base + Index + Scale + Displacement
        // %rax=0x1000, %rcx=0x10. Address: 0x1000 + 0x10*4 + 8 = 0x1048
        MemoryOperand op = new MemoryOperand("8(%rax,%rcx,4)");
        String desc = op.getDescription(state, labelManager);

        assertTrue(desc.contains("0x1048"));
        assertTrue(desc.contains("displacement[8] + base[%rax] + (index[%rcx] * scale[4])"));
    }

    @Test
    public void testGetDescriptionRIPRelative() {
        // RIP-relative with label
        MemoryOperand opLabel = new MemoryOperand("my_var(%rip)");
        assertEquals("memory at effective address 0x3000 (resolved as: RIP-relative label my_var)",
                opLabel.getDescription(state, labelManager));

        // RIP-relative with raw offset
        MemoryOperand opRaw = new MemoryOperand("3(%rip)");
        String desc = opRaw.getDescription(state, labelManager);
        assertTrue(desc.contains("resolved as: %rip + 3"));
    }

    @Test
    public void testGetDescriptionBaseOnly() {
        // Just (%rax) -> Address 0x1000
        MemoryOperand op = new MemoryOperand("(%rax)");
        assertEquals("memory at effective address 0x1000 (resolved as: displacement[0] + base[%rax])",
                op.getDescription(state, labelManager));
    }

    @Test
    public void testGetDescriptionBaseIndexDisplacement() {
        MemoryOperand op = new MemoryOperand("2(%rax,%rcx)");
        assertTrue(op.getDescription(state, labelManager)
                .contains("resolved as: displacement[2] + base[%rax] + (index[%rcx] * scale[1])"));
    }
}