package io.github.AaditS22.asmvisualizer.backend.instructions.operands;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterOperandTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    public void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();

        state.setRegister("rax", 8, 0x1000);
        state.setRegister("rbx", 8, 0x2000);
        state.setRegister("rcx", 8, 0x10);
    }

    @Test
    void wrongRegisterNameTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            RegisterOperand ro = new RegisterOperand("%abc");
        });
    }

    @Test
    void sizeMismatchTest() {
        RegisterOperand ro = new RegisterOperand("%rax");
        assertThrows(IllegalArgumentException.class, () -> {
            ro.getValue(state, labelManager, Size.LONG);
        });
    }

    @Test
    void getValueTest() {
        RegisterOperand ro = new RegisterOperand("%rax");
        long value = ro.getValue(state, labelManager, Size.QUAD);
        assertEquals(0x1000, value);

        RegisterOperand ro2 = new RegisterOperand("%al");
        value = ro2.getValue(state, labelManager, Size.BYTE);
        assertEquals(0, value);
    }

    @Test
    void setValueTest() {
        RegisterOperand ro = new RegisterOperand("%rax");
        ro.setValue(state, labelManager, 0x2000, Size.QUAD);
        assertEquals(0x2000, state.getRegister("rax", 8));

        RegisterOperand ro2 = new RegisterOperand("%al");
        ro2.setValue(state, labelManager, 0x10, Size.BYTE);
        assertEquals(0x2010, state.getRegister("rax", 8));
    }
}