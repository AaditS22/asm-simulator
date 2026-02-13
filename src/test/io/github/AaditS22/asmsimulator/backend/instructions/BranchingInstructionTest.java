package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.CallInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.JmpInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.RetInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BranchingInstructionTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();

        labelManager.addCodeLabel("test", 0x2000L);
    }

    // ==================== JMP Tests ====================

    @Test
    void jmpIllegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                    new ImmediateOperand("$4")));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                    new RegisterOperand("%rax"),
                    new RegisterOperand("%rbx")
            ));
        });
    }

    @Test
    void labelJumpTest() {
        LabelOperand label = new LabelOperand("test");
        JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                label
        ));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void registerValueJumpTest() {
        state.setRegister("rax", 8, 0xABCDEFL);
        JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        instruction.execute(state, labelManager);
        assertEquals(0xABCDEFL, state.getPC());
    }

    @Test
    void memoryValueJumpTest() {
        state.getMemory().writeQuad(0x1000L, 0xABCDEFL);
        state.setRegister("rax", 8, 0x1000L);
        JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                new MemoryOperand("(%rax)")
        ));
        instruction.execute(state, labelManager);
        assertEquals(0xABCDEFL, state.getPC());
    }

    @Test
    void getDescriptionTest() {
        LabelOperand label = new LabelOperand("test");
        JmpInstruction instruction = new JmpInstruction("jmp", Size.QUAD, List.of(
                label
        ));
        assertTrue(instruction.getDescription(state, labelManager).contains("0x2000"));
    }

    // ==================== CALL Tests ====================

    @Test
    void callIllegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            CallInstruction instruction = new CallInstruction("jmp", Size.QUAD, List.of(
                    new ImmediateOperand("$4")));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CallInstruction instruction = new CallInstruction("jmp", Size.QUAD, List.of(
                    new RegisterOperand("%rax"),
                    new RegisterOperand("%rbx")
            ));
        });
    }

    @Test
    void callReturnAddressPushTest() {
        state.setPC(0x1000L);
        CallInstruction instruction = new CallInstruction("call", Size.QUAD, List.of(
                new LabelOperand("test")
        ));
        instruction.execute(state, labelManager);
        long rsp = state.getRegister("rsp", 8);
        assertEquals(0x1008L, state.getMemory().readQuad(rsp));
    }

    @Test
    void callUpdatesPCTest() {
        state.setPC(0x1000L);
        CallInstruction instruction = new CallInstruction("call", Size.QUAD, List.of(
                new LabelOperand("test")
        ));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void callDescriptionTest() {
        LabelOperand label = new LabelOperand("test");
        CallInstruction instruction = new CallInstruction("call", Size.QUAD, List.of(
                label
        ));
        assertTrue(instruction.getDescription(state, labelManager).contains("0x2000"));
    }

    // ==================== RET Tests ====================

    @Test
    void retIllegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RetInstruction("ret", Size.QUAD, List.of(
                    new ImmediateOperand("$4")));
        });
    }

    @Test
    void retPopsStackTest() {
        state.setRegister("rsp", 8, 0x1000L);
        state.getMemory().writeQuad(0x1000L, 0x2000L);
        RetInstruction instruction = new RetInstruction("ret", Size.QUAD, List.of());
        instruction.execute(state, labelManager);
        assertEquals(0x1008L, state.getRegister("rsp", 8));
    }

    @Test
    void retUpdatesPCTest() {
        state.setRegister("rsp", 8, 0x1000L);
        state.getMemory().writeQuad(0x1000L, 0x2000L);
        RetInstruction instruction = new RetInstruction("ret", Size.QUAD, List.of());
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void retDescriptionTest() {
        RetInstruction instruction = new RetInstruction("ret", Size.QUAD, List.of());
        assertEquals("Pops the return address from the top of the stack and jumps to it",
                instruction.getDescription(state, labelManager));
    }
}