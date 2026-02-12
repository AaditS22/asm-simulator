package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImmediateOperandTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    public void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();

        // Set up test labels
        labelManager.addDataLabel("my_var", 0x3000, 8);
        labelManager.addDataLabel("array_start", 0x4000, 8);
        state.getMemory().writeQuad(0x3000, 0x5555555555555555L);
        state.getMemory().writeQuad(0x4000, 0xAAAAAAAAAAAAAAAAL);
    }

    @Test
    void standardNumberTest() {
        ImmediateOperand io = new ImmediateOperand("$100");
        assertEquals(100L, io.getValue(state, labelManager, null));

        io = new ImmediateOperand("$0x100");
        assertEquals(256, io.getValue(state, labelManager, null));
    }

    @Test
    void exceptionTest() {
        ImmediateOperand io = new ImmediateOperand("$abcd");
        assertThrows(IllegalArgumentException.class, () -> {
            io.getValue(state, labelManager, null);
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            io.setValue(state, labelManager, 0, null);
        });
    }

    @Test
    void labelTest() {
        ImmediateOperand io = new ImmediateOperand("$my_var");
        assertEquals(0x3000, io.getValue(state, labelManager, null));

        ImmediateOperand io2 = new ImmediateOperand("$array_start");
        assertEquals(0x4000, io2.getValue(state, labelManager, null));
    }

    @Test
    void toAssemblyStringTest() {
        assertEquals("$100", new ImmediateOperand("$100").toAssemblyString());
        assertEquals("$0x20", new ImmediateOperand("$0x20").toAssemblyString());
        assertEquals("$my_var", new ImmediateOperand("$my_var").toAssemblyString());
    }

    @Test
    void getDescriptionTest() {
        ImmediateOperand io = new ImmediateOperand("$100");
        assertEquals("immediate value $100 (100, 0x64)", io.getDescription(state, labelManager));

        ImmediateOperand ioLabel = new ImmediateOperand("$my_var");
        assertEquals("immediate value $my_var (12288, 0x3000)", ioLabel.getDescription(state, labelManager));
    }
}