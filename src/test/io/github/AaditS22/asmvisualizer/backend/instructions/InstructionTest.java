package io.github.AaditS22.asmvisualizer.backend.instructions;

import io.github.AaditS22.asmvisualizer.backend.cpu.CPUState;
import io.github.AaditS22.asmvisualizer.backend.cpu.LabelManager;
import io.github.AaditS22.asmvisualizer.backend.instructions.arithmetic.AddInstruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.arithmetic.DecInstruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.arithmetic.IncInstruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.arithmetic.SubInstruction;
import io.github.AaditS22.asmvisualizer.backend.instructions.movement.*;
import io.github.AaditS22.asmvisualizer.backend.instructions.operands.*;
import io.github.AaditS22.asmvisualizer.backend.util.MemoryLayout;
import io.github.AaditS22.asmvisualizer.backend.util.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Disclaimer: Some of the following tests were written by LLMs

class InstructionTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();
    }

    // ==================== MOV Tests ====================

    @Test
    void movImmediateToRegisterTest() {
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new ImmediateOperand("$42"),
                new RegisterOperand("%rax")
        ));
        mov.execute(state, labelManager);
        assertEquals(42L, state.getRegister("rax", 8));
    }

    @Test
    void movRegisterToRegisterTest() {
        state.setRegister("rax", 8, 0xDEADBEEFL);
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        ));
        mov.execute(state, labelManager);
        assertEquals(0xDEADBEEFL, state.getRegister("rbx", 8));
    }

    @Test
    void movRegisterToMemoryTest() {
        state.setRegister("rax", 8, 0xCAFE);
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        ));
        mov.execute(state, labelManager);
        assertEquals(0xCAFE, state.getMemory().readQuad(0x5000L));
    }

    @Test
    void movMemoryToRegisterTest() {
        state.getMemory().writeQuad(0x5000L, 0xBEEF);
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%rax")
        ));
        mov.execute(state, labelManager);
        assertEquals(0xBEEF, state.getRegister("rax", 8));
    }

    @Test
    void movAdvancesPCTest() {
        long pcBefore = state.getPC();
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new ImmediateOperand("$1"),
                new RegisterOperand("%rax")
        ));
        mov.execute(state, labelManager);
        assertEquals(pcBefore + 8, state.getPC());
    }

    @Test
    void movWrongOperandCountTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new MovInstruction("movq", Size.QUAD, List.of(
                        new RegisterOperand("%rax")
                ))
        );
    }

    @Test
    void movDifferentSizesTest() {
        state.setRegister("rax", 8, 0L);
        Instruction movByte = new MovInstruction("movb", Size.BYTE, List.of(
                new ImmediateOperand("$0xFF"),
                new RegisterOperand("%al")
        ));
        movByte.execute(state, labelManager);
        assertEquals(0xFFL, state.getRegister("rax", 8));

        Instruction movWord = new MovInstruction("movw", Size.WORD, List.of(
                new ImmediateOperand("$0xABCD"),
                new RegisterOperand("%ax")
        ));
        movWord.execute(state, labelManager);
        assertEquals(0xABCDL, state.getRegister("rax", 8));
    }

    // ==================== LEA Tests ====================

    @Test
    void leaSimpleDisplacementTest() {
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%rax")
        ));
        lea.execute(state, labelManager);
        assertEquals(0x5000L, state.getRegister("rax", 8));
    }

    @Test
    void leaBaseDisplacementTest() {
        state.setRegister("rbx", 8, 0x1000L);
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("16(%rbx)"),
                new RegisterOperand("%rax")
        ));
        lea.execute(state, labelManager);
        assertEquals(0x1000L + 16, state.getRegister("rax", 8));
    }

    @Test
    void leaBaseIndexScaleTest() {
        state.setRegister("rbx", 8, 0x1000L);
        state.setRegister("rcx", 8, 5L);
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("8(%rbx,%rcx,4)"),
                new RegisterOperand("%rax")
        ));
        lea.execute(state, labelManager);
        // address = 8 + 0x1000 + (5 * 4) = 8 + 4096 + 20 = 4124
        assertEquals(4124L, state.getRegister("rax", 8));
    }

    @Test
    void leaDoesNotReadMemoryTest() {
        // LEA should compute the address, not load the value at that address
        state.setRegister("rbx", 8, 0x5000L);
        state.getMemory().writeQuad(0x5000L, 0xDEADBEEFL);
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("(%rbx)"),
                new RegisterOperand("%rax")
        ));
        lea.execute(state, labelManager);
        // Should store the address (0x5000), not the value at 0x5000 (0xDEADBEEF)
        assertEquals(0x5000L, state.getRegister("rax", 8));
    }

    @Test
    void leaRequiresMemorySourceTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeaInstruction("leaq", Size.QUAD, List.of(
                        new RegisterOperand("%rax"),
                        new RegisterOperand("%rbx")
                ))
        );
    }

    @Test
    void leaRequiresRegisterDestTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeaInstruction("leaq", Size.QUAD, List.of(
                        new MemoryOperand("0x5000"),
                        new MemoryOperand("0x6000")
                ))
        );
    }

    // ==================== PUSH/POP Tests ====================

    @Test
    void pushRegistersOntoStackTest() {
        long rspBefore = state.getRegister("rsp", 8);
        state.setRegister("rax", 8, 100L);
        Instruction push = new PushInstruction("pushq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        push.execute(state, labelManager);
        assertEquals(rspBefore - 8, state.getRegister("rsp", 8));
        assertEquals(100L, state.getMemory().readQuad(state.getRegister("rsp", 8)));
    }

    @Test
    void pushImmediateTest() {
        long rspBefore = state.getRegister("rsp", 8);
        Instruction push = new PushInstruction("pushq", Size.QUAD, List.of(
                new ImmediateOperand("$999")
        ));
        push.execute(state, labelManager);
        assertEquals(rspBefore - 8, state.getRegister("rsp", 8));
        assertEquals(999L, state.getMemory().readQuad(state.getRegister("rsp", 8)));
    }

    @Test
    void popIntoRegisterTest() {
        // Push a value first, then pop it
        state.setRegister("rax", 8, 0xABCDL);
        new PushInstruction("pushq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        )).execute(state, labelManager);

        long rspAfterPush = state.getRegister("rsp", 8);
        Instruction pop = new PopInstruction("popq", Size.QUAD, List.of(
                new RegisterOperand("%rbx")
        ));
        pop.execute(state, labelManager);
        assertEquals(0xABCDL, state.getRegister("rbx", 8));
        assertEquals(rspAfterPush + 8, state.getRegister("rsp", 8));
    }

    @Test
    void pushPopMultipleValuesTest() {
        state.setRegister("rax", 8, 10L);
        state.setRegister("rbx", 8, 20L);
        state.setRegister("rcx", 8, 30L);

        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%rax")))
                .execute(state, labelManager);
        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%rbx")))
                .execute(state, labelManager);
        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%rcx")))
                .execute(state, labelManager);

        // Pop in reverse order (LIFO)
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%r8")))
                .execute(state, labelManager);
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%r9")))
                .execute(state, labelManager);
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%r10")))
                .execute(state, labelManager);

        assertEquals(30L, state.getRegister("r8", 8));
        assertEquals(20L, state.getRegister("r9", 8));
        assertEquals(10L, state.getRegister("r10", 8));
    }

    @Test
    void pushWrongOperandCountTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new PushInstruction("pushq", Size.QUAD, List.of(
                        new RegisterOperand("%rax"),
                        new RegisterOperand("%rbx")
                ))
        );
    }

    // ==================== MOVZB Tests ====================

    @Test
    void movzbZeroExtendsTest() {
        // Set rax to a large value, then movzb a byte into it - upper bits should clear
        state.setRegister("rbx", 8, 0xFFFFFFFF_FFFFFFFFL);
        state.setRegister("rax", 8, 0xAB);
        Instruction movzb = new MovzbInstruction("movzbl", Size.BYTE, List.of(
                new RegisterOperand("%al"),
                new RegisterOperand("%ebx")
        ));
        movzb.execute(state, labelManager);
        // rbx should be zero-extended: only the low byte should remain
        assertEquals(0xABL, state.getRegister("rbx", 8));
    }

    @Test
    void movzbFromMemoryTest() {
        state.getMemory().writeByte(0x5000L, (byte) 0x7F);
        Instruction movzb = new MovzbInstruction("movzbl", Size.BYTE, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%eax")
        ));
        movzb.execute(state, labelManager);
        assertEquals(0x7FL, state.getRegister("rax", 8));
    }

    @Test
    void movzbRequiresRegisterDestTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new MovzbInstruction("movzbl", Size.BYTE, List.of(
                        new RegisterOperand("%al"),
                        new MemoryOperand("0x5000")
                ))
        );
    }

    // ==================== MOVZW Tests ====================

    @Test
    void movzwZeroExtendsTest() {
        state.setRegister("rbx", 8, 0xFFFFFFFF_FFFFFFFFL);
        state.setRegister("rax", 8, 0xABCD);
        Instruction movzw = new MovzwInstruction("movzwl", Size.WORD, List.of(
                new RegisterOperand("%ax"),
                new RegisterOperand("%ebx")
        ));
        movzw.execute(state, labelManager);
        assertEquals(0xABCDL, state.getRegister("rbx", 8));
    }

    @Test
    void movzwFromMemoryTest() {
        state.getMemory().writeWord(0x5000L, (short) 0x1234);
        Instruction movzw = new MovzwInstruction("movzwl", Size.WORD, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%eax")
        ));
        movzw.execute(state, labelManager);
        assertEquals(0x1234L, state.getRegister("rax", 8));
    }

    @Test
    void movzwRequiresRegisterDestTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new MovzwInstruction("movzwl", Size.WORD, List.of(
                        new RegisterOperand("%ax"),
                        new MemoryOperand("0x5000")
                ))
        );
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
        assertTrue(add.getDescription(state, labelManager).contains("Adds"));
    }

    @Test
    void subDescriptionTest() {
        state.getMemory().writeQuad(0x5000L, 10L);
        state.setRegister("rax", 8, 20L);
        Instruction sub = new SubInstruction("subq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new MemoryOperand("0x5000")
        ));
        assertTrue(sub.getDescription(state, labelManager).contains("Subtracts"));
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
        assertTrue(inc.getDescription(state, labelManager).contains("Increments"));
    }

    @Test
    void decDescriptionTest() {
        state.setRegister("rax", 8, 10L);
        Instruction dec = new DecInstruction("decq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        assertTrue(dec.getDescription(state, labelManager).contains("Decrements"));
    }

    // ==================== Combined Simulation Tests ====================

    @Test
    void functionPrologueEpilogueTest() {
        // Simulates a typical function call setup:
        //   pushq %rbp
        //   movq  %rsp, %rbp
        //   ... (function body) ...
        //   popq  %rbp

        long originalRsp = state.getRegister("rsp", 8);
        long originalRbp = state.getRegister("rbp", 8);

        // pushq %rbp
        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%rbp")))
                .execute(state, labelManager);
        assertEquals(originalRsp - 8, state.getRegister("rsp", 8));

        // movq %rsp, %rbp
        new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rsp"),
                new RegisterOperand("%rbp")
        )).execute(state, labelManager);
        assertEquals(state.getRegister("rsp", 8), state.getRegister("rbp", 8));

        long frameBasePointer = state.getRegister("rbp", 8);

        // popq %rbp (epilogue)
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%rbp")))
                .execute(state, labelManager);
        assertEquals(originalRbp, state.getRegister("rbp", 8));
        assertEquals(frameBasePointer + 8, state.getRegister("rsp", 8));
    }

    @Test
    void leaForArithmeticTest() {
        // LEA is often used for quick arithmetic: rax = rbx + rcx * 2 + 10
        state.setRegister("rbx", 8, 100L);
        state.setRegister("rcx", 8, 50L);
        new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("10(%rbx,%rcx,2)"),
                new RegisterOperand("%rax")
        )).execute(state, labelManager);
        // 10 + 100 + (50 * 2) = 210
        assertEquals(210L, state.getRegister("rax", 8));
    }

    @Test
    void movChainTest() {
        // Move a value through multiple registers: imm -> rax -> rbx -> memory -> rcx
        new MovInstruction("movq", Size.QUAD, List.of(
                new ImmediateOperand("$0xCAFEBABE"),
                new RegisterOperand("%rax")
        )).execute(state, labelManager);

        new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        )).execute(state, labelManager);

        new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rbx"),
                new MemoryOperand("0x6000")
        )).execute(state, labelManager);

        new MovInstruction("movq", Size.QUAD, List.of(
                new MemoryOperand("0x6000"),
                new RegisterOperand("%rcx")
        )).execute(state, labelManager);

        assertEquals(0xCAFEBABEL, state.getRegister("rax", 8));
        assertEquals(0xCAFEBABEL, state.getRegister("rbx", 8));
        assertEquals(0xCAFEBABEL, state.getRegister("rcx", 8));
        assertEquals(0xCAFEBABEL, state.getMemory().readQuad(0x6000L));
    }

    @Test
    void leaComputesThenMovStoresTest() {
        // Use LEA to compute an array element address, then MOV to store a value there
        //   leaq (%rbx,%rcx,8), %rax     # rax = base + index*8
        //   movq %rdx, (%rax)            # store value at computed address

        long arrayBase = 0x7000L;
        state.setRegister("rbx", 8, arrayBase);
        state.setRegister("rcx", 8, 3L);       // index 3
        state.setRegister("rdx", 8, 0xAAAAL);   // value to store

        new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("(%rbx,%rcx,8)"),
                new RegisterOperand("%rax")
        )).execute(state, labelManager);

        assertEquals(arrayBase + 3 * 8, state.getRegister("rax", 8));

        new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rdx"),
                new MemoryOperand("(%rax)")
        )).execute(state, labelManager);

        assertEquals(0xAAAAL, state.getMemory().readQuad(arrayBase + 24));
    }

    @Test
    void saveAndRestoreRegistersTest() {
        // Simulates callee-saved register preservation:
        //   pushq %rbx
        //   pushq %r12
        //   movq  $0xFF, %rbx    # use rbx in function body
        //   movq  $0xAA, %r12    # use r12 in function body
        //   popq  %r12
        //   popq  %rbx

        state.setRegister("rbx", 8, 0x1111L);
        state.setRegister("r12", 8, 0x2222L);

        // Save
        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%rbx")))
                .execute(state, labelManager);
        new PushInstruction("pushq", Size.QUAD, List.of(new RegisterOperand("%r12")))
                .execute(state, labelManager);

        // Clobber with function body work
        new MovInstruction("movq", Size.QUAD, List.of(
                new ImmediateOperand("$0xFF"), new RegisterOperand("%rbx")
        )).execute(state, labelManager);
        new MovInstruction("movq", Size.QUAD, List.of(
                new ImmediateOperand("$0xAA"), new RegisterOperand("%r12")
        )).execute(state, labelManager);

        assertEquals(0xFFL, state.getRegister("rbx", 8));
        assertEquals(0xAAL, state.getRegister("r12", 8));

        // Restore (reverse pop order)
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%r12")))
                .execute(state, labelManager);
        new PopInstruction("popq", Size.QUAD, List.of(new RegisterOperand("%rbx")))
                .execute(state, labelManager);

        assertEquals(0x1111L, state.getRegister("rbx", 8));
        assertEquals(0x2222L, state.getRegister("r12", 8));
    }

    @Test
    void movzbAfterByteLoadTest() {
        // Store a byte at memory, load it with movzb to get a clean 32/64-bit value
        state.getMemory().writeByte(0x5000L, (byte) 0xFE);
        state.setRegister("rax", 8, 0xFFFFFFFF_FFFFFFFFL); // fill rax with all 1s

        new MovzbInstruction("movzbl", Size.BYTE, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%eax")
        )).execute(state, labelManager);

        // Only the low byte should survive, upper bits zeroed
        assertEquals(0xFEL, state.getRegister("rax", 8));
    }

    @Test
    void pcAdvancesEachInstructionTest() {
        long pc = state.getPC();
        Instruction[] instructions = {
                new MovInstruction("movq", Size.QUAD, List.of(
                        new ImmediateOperand("$1"), new RegisterOperand("%rax"))),
                new MovInstruction("movq", Size.QUAD, List.of(
                        new ImmediateOperand("$2"), new RegisterOperand("%rbx"))),
                new PushInstruction("pushq", Size.QUAD, List.of(
                        new RegisterOperand("%rax"))),
                new PopInstruction("popq", Size.QUAD, List.of(
                        new RegisterOperand("%rcx"))),
        };
        for (int i = 0; i < instructions.length; i++) {
            assertEquals(pc + (i * 8), state.getPC());
            instructions[i].execute(state, labelManager);
        }
        assertEquals(pc + (instructions.length * 8), state.getPC());
    }

    // ==================== Description Tests ====================

    @Test
    void movDescriptionTest() {
        state.setRegister("rax", 8, 42L);
        Instruction mov = new MovInstruction("movq", Size.QUAD, List.of(
                new RegisterOperand("%rax"),
                new RegisterOperand("%rbx")
        ));
        String desc = mov.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Moves"));
        assertTrue(desc.contains("%rax"));
        assertTrue(desc.contains("%rbx"));
    }

    @Test
    void leaDescriptionTest() {
        state.setRegister("rbx", 8, 0x1000L);
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("16(%rbx)"),
                new RegisterOperand("%rax")
        ));
        String desc = lea.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Loads"));
        assertTrue(desc.contains("%rax"));
    }

    @Test
    void pushDescriptionTest() {
        state.setRegister("rax", 8, 42L);
        Instruction push = new PushInstruction("pushq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        String desc = push.getDescription(state, labelManager);
        assertTrue(desc.contains("Pushes"));
        assertTrue(desc.contains("stack"));
        assertTrue(desc.contains("%rax"));
    }

    @Test
    void popDescriptionTest() {
        // Push something first so there's a value to pop
        state.getMemory().stackPush(42L);
        Instruction pop = new PopInstruction("popq", Size.QUAD, List.of(
                new RegisterOperand("%rax")
        ));
        String desc = pop.getDescription(state, labelManager);
        assertTrue(desc.contains("Pops"));
        assertTrue(desc.contains("stack"));
        assertTrue(desc.contains("%rax"));
    }

    @Test
    void leaStripsMemoryAtFromDescriptionTest() {
        // LEA always strips "memory at " from the source
        state.setRegister("rbx", 8, 0x1000L);
        Instruction lea = new LeaInstruction("leaq", Size.QUAD, List.of(
                new MemoryOperand("16(%rbx)"),
                new RegisterOperand("%rax")
        ));
        String desc = lea.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Loads"));
        assertFalse(desc.contains("memory at"));
        assertTrue(desc.contains("effective address"));
        assertTrue(desc.contains("%rax"));
    }

    @Test
    void movzbRegisterSourceDescriptionTest() {
        state.setRegister("rax", 8, 0xAB);
        Instruction movzb = new MovzbInstruction("movzbl", Size.BYTE, List.of(
                new RegisterOperand("%al"),
                new RegisterOperand("%ebx")
        ));
        String desc = movzb.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Moves"));
        assertTrue(desc.contains("low byte"));
        assertTrue(desc.contains("%al"));
        assertTrue(desc.contains("%ebx"));
        assertTrue(desc.contains("zero-extended"));
    }

    @Test
    void movzbMemorySourceDescriptionTest() {
        state.getMemory().writeByte(0x5000L, (byte) 0x7F);
        Instruction movzb = new MovzbInstruction("movzbl", Size.BYTE, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%eax")
        ));
        String desc = movzb.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Moves"));
        assertTrue(desc.contains("low byte"));
        assertTrue(desc.contains("memory at"));
        assertTrue(desc.contains("%eax"));
        assertTrue(desc.contains("zero-extended"));
    }

    @Test
    void movzwRegisterSourceDescriptionTest() {
        state.setRegister("rax", 8, 0xABCD);
        Instruction movzw = new MovzwInstruction("movzwl", Size.WORD, List.of(
                new RegisterOperand("%ax"),
                new RegisterOperand("%ebx")
        ));
        String desc = movzw.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Moves"));
        assertTrue(desc.contains("low word"));
        assertTrue(desc.contains("%ax"));
        assertTrue(desc.contains("%ebx"));
        assertTrue(desc.contains("zero-extended"));
    }

    @Test
    void movzwMemorySourceDescriptionTest() {
        state.getMemory().writeWord(0x5000L, (short) 0x1234);
        Instruction movzw = new MovzwInstruction("movzwl", Size.WORD, List.of(
                new MemoryOperand("0x5000"),
                new RegisterOperand("%eax")
        ));
        String desc = movzw.getDescription(state, labelManager);
        assertTrue(desc.startsWith("Moves"));
        assertTrue(desc.contains("low word"));
        assertTrue(desc.contains("memory at"));
        assertTrue(desc.contains("%eax"));
        assertTrue(desc.contains("zero-extended"));
    }
}
