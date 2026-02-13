package io.github.AaditS22.asmsimulator.backend.instructions.operands;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelOperandTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    public void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();

        // Set up test labels
        labelManager.addDataLabel("my_var", 0x3000, 8);
        labelManager.addDataLabel("my_var2", 0x4000, 8);
        labelManager.addCodeLabel("my_code_label", 0x1000);
        state.getMemory().writeQuad(0x3000, 0x5555555555555555L);
        state.getMemory().writeQuad(0x4000, 0xAAAAAAAAAAAAAAAAL);
    }

    @Test
    void getDataLabelTest() {
        LabelOperand lo = new LabelOperand("my_var");
        assertEquals(0x3000, lo.getValue(state, labelManager, Size.BYTE));
    }

    @Test
    void getCodeLabelTest() {
        LabelOperand lo = new LabelOperand("my_code_label");
        assertEquals(0x1000, lo.getValue(state, labelManager, Size.BYTE));
    }

    @Test
    void testSetException() {
        LabelOperand lo = new LabelOperand("my_var");
        assertThrows(UnsupportedOperationException.class,
                () -> lo.setValue(state, labelManager, 0, Size.BYTE));
    }

    @Test
    void toAssemblyStringTest() {
        assertEquals("my_var", new LabelOperand("my_var").toAssemblyString());
    }
}