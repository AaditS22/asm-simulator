package io.github.AaditS22.asmsimulator.backend.instructions;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.*;
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
        assertTrue(instruction.getDescription(state, labelManager).contains("the label 'test'"));
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
        assertTrue(instruction.getDescription(state, labelManager).contains("the label 'test'"));
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
        assertEquals("Popped the return address from the top of the stack and jumped to it",
                instruction.getDescription(state, labelManager));
    }

    // ==================== Conditional Jump Tests ====================

    @Test
    void conditionalJumpIllegalArgumentTest() {
        // Should reject non-label operands
        assertThrows(IllegalArgumentException.class, () -> {
            new ConditionalJumpInstruction("je", Size.QUAD, List.of(
                    new RegisterOperand("%rax")));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new ConditionalJumpInstruction("jne", Size.QUAD, List.of(
                    new MemoryOperand("(%rax)")));
        });
        // Should reject wrong operand count
        assertThrows(IllegalArgumentException.class, () -> {
            new ConditionalJumpInstruction("je", Size.QUAD, List.of());
        });
    }

    @Test
    void jeJumpsWhenZeroFlagSet() {
        state.getFlags().updateSubFlags(5, 5, 64); // 5 - 5 = 0, ZF=1
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "je", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jeFallsThroughWhenZeroFlagClear() {
        long pcBefore = state.getPC();
        state.getFlags().updateSubFlags(5, 3, 64); // 5 - 3 = 2, ZF=0
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "je", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void jneJumpsWhenNotEqual() {
        state.getFlags().updateSubFlags(5, 3, 64); // ZF=0
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jne", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jneFallsThroughWhenEqual() {
        state.getFlags().updateSubFlags(5, 5, 64); // ZF=1
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jne", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void jgJumpsWhenGreater() {
        state.getFlags().updateSubFlags(10, 3, 64); // 10 > 3: ZF=0, SF=OF
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jg", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jgFallsThroughWhenEqual() {
        state.getFlags().updateSubFlags(5, 5, 64); // ZF=1, so not "greater"
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jg", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void jgeJumpsWhenEqual() {
        state.getFlags().updateSubFlags(5, 5, 64); // SF=OF (both false here)
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jge", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jlJumpsWhenLess() {
        state.getFlags().updateSubFlags(3, 10, 64); // 3 < 10: SF≠OF
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jl", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jlFallsThroughWhenGreater() {
        state.getFlags().updateSubFlags(10, 3, 64); // SF=OF
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jl", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void jleJumpsWhenLess() {
        state.getFlags().updateSubFlags(3, 10, 64); // 3 < 10
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jle", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jleJumpsWhenEqual() {
        state.getFlags().updateSubFlags(5, 5, 64); // ZF=1
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jle", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void jleFallsThroughWhenGreater() {
        state.getFlags().updateSubFlags(10, 3, 64);
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "jle", Size.QUAD, List.of(new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void conditionalJumpDescriptionTest() {
        state.getFlags().updateSubFlags(5, 5, 64);
        ConditionalJumpInstruction instruction = new ConditionalJumpInstruction(
                "je", Size.QUAD, List.of(new LabelOperand("test")));
        String desc = instruction.getDescription(state, labelManager);
        assertTrue(desc.contains("the last comparison was equal"));
    }

// ==================== Loop Tests ====================

    @Test
    void loopIllegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LoopInstruction("loop", Size.QUAD, List.of(
                    new RegisterOperand("%rax")));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new LoopInstruction("loop", Size.QUAD, List.of());
        });
    }

    @Test
    void loopJumpsWhenRcxNotZero() {
        state.setRegister("rcx", 8, 5L);
        LoopInstruction instruction = new LoopInstruction("loop", Size.QUAD, List.of(
                new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(4L, state.getRegister("rcx", 8));
        assertEquals(0x2000L, state.getPC());
    }

    @Test
    void loopFallsThroughWhenRcxReachesZero() {
        long pcBefore = state.getPC();
        state.setRegister("rcx", 8, 1L);
        LoopInstruction instruction = new LoopInstruction("loop", Size.QUAD, List.of(
                new LabelOperand("test")));
        instruction.execute(state, labelManager);
        assertEquals(0L, state.getRegister("rcx", 8));
        assertNotEquals(0x2000L, state.getPC());
    }

    @Test
    void loopDoesNotModifyFlags() {
        state.setRegister("rcx", 8, 1L);
        // Set some flags via a subtraction
        state.getFlags().updateSubFlags(5, 5, 64); // ZF=1
        assertTrue(state.getFlags().isZero());

        LoopInstruction instruction = new LoopInstruction("loop", Size.QUAD, List.of(
                new LabelOperand("test")));
        instruction.execute(state, labelManager);

        // rcx went 1 → 0, but ZF should still reflect the earlier cmp, not the decrement
        assertTrue(state.getFlags().isZero());
    }

    @Test
    void loopDescriptionTest() {
        state.setRegister("rcx", 8, 3L);
        LoopInstruction instruction = new LoopInstruction("loop", Size.QUAD, List.of(
                new LabelOperand("test")));
        String desc = instruction.getDescription(state, labelManager);
        assertTrue(desc.contains("the label 'test'"));
    }
}