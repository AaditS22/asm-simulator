package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovInstruction;
import io.github.AaditS22.asmsimulator.backend.util.DataLabel;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    private CPUState state;
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        state = new CPUState();
        labelManager = new LabelManager();
    }

    // ==================== Basic Instruction Parsing ====================

    @Test
    void parseSimpleInstructionsTest() {
        String input = """
                .text
                .globl main
                main:
                    movq $5, %rax
                    addq $10, %rax
                    ret
                """;

        Parser.ParseResult result = Parser.parse(input, state, labelManager);

        assertEquals("main", result.entryPoint());
        List<Instruction> instructions = result.instructions();
        assertEquals(3, instructions.size());
        assertInstanceOf(MovInstruction.class, instructions.get(0));
        assertTrue(labelManager.isCodeLabel("main"));
        assertEquals(MemoryLayout.CODE_BASE, labelManager.getCodeLabel("main"));
    }

    @Test
    void parseCommentsAndWhitespaceTest() {
        String input = """
                # This is a comment
                .text
                    
                start:      # Inline comment
                    movq $1, %rax
                
                # Another comment
                """;

        Parser.ParseResult result = Parser.parse(input, state, labelManager);
        assertEquals(1, result.instructions().size());
        assertTrue(labelManager.isCodeLabel("start"));
    }

    // ==================== Data Directives & Memory ====================

    @Test
    void parseDataDirectivesTest() {
        String input = """
                .data
                val1: .quad 0xDEADBEEF
                val2: .byte 0xFF, 0xAA
                val3: .word 0x1234
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();

        // Check val1 (Quad)
        assertTrue(labelManager.isDataLabel("val1"));
        long val1Addr = labelManager.getDataLabel("val1").address();
        assertEquals(MemoryLayout.DATA_BASE, val1Addr);
        assertEquals(0xDEADBEEFL, memory.readQuad(val1Addr));

        // Check val2 (Bytes)
        long val2Addr = labelManager.getDataLabel("val2").address();
        assertEquals(val1Addr + 8, val2Addr);
        assertEquals((byte) 0xFF, memory.readByte(val2Addr));
        assertEquals((byte) 0xAA, memory.readByte(val2Addr + 1));

        // Check val3 (Word)
        long val3Addr = labelManager.getDataLabel("val3").address();
        assertEquals(val2Addr + 2, val3Addr);
        assertEquals((short) 0x1234, memory.readWord(val3Addr));
    }

    @Test
    void parseStringDirectivesTest() {
        String input = """
                .rodata
                str1: .ascii "Hi"
                str2: .asciz "Hello"
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();
        long base = MemoryLayout.READ_ONLY_DATA_BASE;

        // Check .ascii "Hi" (no null terminator)
        assertEquals('H', (char) memory.readByte(base).byteValue());
        assertEquals('i', (char) memory.readByte(base + 1).byteValue());

        // Check .asciz "Hello" (with null terminator) - starts at base + 2
        long str2Addr = base + 2;
        assertEquals('H', (char) memory.readByte(str2Addr).byteValue());
        assertEquals('e', (char) memory.readByte(str2Addr + 1).byteValue());
        assertEquals((byte) 0, memory.readByte(str2Addr + 5)); // Null terminator
    }

    @Test
    void parseEscapedStringsTest() {
        String input = """
                .data
                str: .asciz "A\\nB"
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();
        long base = MemoryLayout.DATA_BASE;

        assertEquals('A', (char) memory.readByte(base).byteValue());
        assertEquals('\n', (char) memory.readByte(base + 1).byteValue());
        assertEquals('B', (char) memory.readByte(base + 2).byteValue());
        assertEquals((byte) 0, memory.readByte(base + 3));
    }

    @Test
    void parseZeroAndSpaceTest() {
        String input = """
                .bss
                arr: .zero 10
                end: .quad 1
                """;

        Parser.parse(input, state, labelManager);

        DataLabel arr = labelManager.getDataLabel("arr");
        DataLabel end = labelManager.getDataLabel("end");

        assertEquals(MemoryLayout.BSS_BASE, arr.address());
        // End should be 10 bytes after arr
        assertEquals(MemoryLayout.BSS_BASE + 10, end.address());
    }

    // ==================== Alignment Tests ====================

    @Test
    void parseAlignTest() {
        String input = """
                .data
                .byte 0xFF
                .align 8
                aligned_val: .quad 0x11223344
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();

        // Byte at offset 0
        assertEquals((byte) 0xFF, memory.readByte(MemoryLayout.DATA_BASE));

        // .align 8 should push next address to nearest multiple of 8
        // Next multiple of 8 after 0x...001 is 0x...008
        long alignedAddr = MemoryLayout.DATA_BASE + 8 - (MemoryLayout.DATA_BASE % 8);
        // Note: DATA_BASE is usually aligned to page boundaries, so +1 pushes it off.
        // Let's verify via the label manager where it put 'aligned_val'

        long labelAddr = labelManager.getDataLabel("aligned_val").address();
        assertEquals(0, labelAddr % 8, "Address should be 8-byte aligned");
        assertEquals(0x11223344L, memory.readQuad(labelAddr));
    }

    // ==================== Complex Cases & Label Resolution ====================

    @Test
    void forwardJumpReferenceTest() {
        String input = """
                .text
                    jmp target
                    movq $1, %rax
                target:
                    ret
                """;

        Parser.ParseResult result = Parser.parse(input, state, labelManager);
        List<Instruction> insts = result.instructions();

        // 1. jmp
        // 2. movq
        // 3. ret (at target)

        // JMP is 8 bytes, MOVQ is 8 bytes. Target should be at CODE_BASE + 16
        long expectedTarget = MemoryLayout.CODE_BASE + 16;
        assertEquals(expectedTarget, labelManager.getCodeLabel("target"));
    }

    @Test
    void labelOnSeparateLineTest() {
        String input = """
                .data
                my_var:
                    .quad 100
                """;

        Parser.parse(input, state, labelManager);
        assertTrue(labelManager.isDataLabel("my_var"));
        assertEquals(100L, state.getMemory().readQuad(MemoryLayout.DATA_BASE));
    }

    @Test
    void multiLineLabelsWithPendingTest() {
        // This tests logic where a label is defined but data comes later
        String input = """
                .data
                label1:
                label2:
                    .quad 50
                """;

        Parser.parse(input, state, labelManager);
        // Both labels should point to the same address
        assertEquals(labelManager.getDataLabel("label1").address(),
                labelManager.getDataLabel("label2").address());
        assertEquals(50L, state.getMemory().readQuad(labelManager.getDataLabel("label1").address()));
    }

    // ==================== Error Handling Tests ====================

    @Test
    void invalidInstructionThrowsTest() {
        String input = """
                .text
                invalid_opcode $5, %rax
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
        assertTrue(e.getMessage().contains("Error on line"));
    }

    @Test
    void instructionInWrongSectionThrowsTest() {
        String input = """
                .data
                movq $5, %rax
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
        assertTrue(e.getMessage().contains("Instructions are not allowed"));
    }

    @Test
    void sectionOverflowThrowsTest() {
        // Attempt to allocate huge space
        String input = """
                .data
                .space 0x7FFFFFFF
                """;
        // Assuming DATA section has limits defined in MemoryLayout, this should eventually fail
        // or if limits are strict. MemoryLayout limit is BSS_BASE (0x602000) - DATA_BASE (0x601000) = 4KB usually.
        // 0x7FFFFFFF is definitely larger than 4KB.

        assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
    }

    @Test
    void duplicateLabelThrowsTest() {
        String input = """
                .text
                start:
                    nop
                start:
                    nop
                """;
        assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
    }

    @Test
    void unclosedStringLiteralThrowsTest() {
        String input = """
                .data
                .ascii "Unclosed string
                """;
        assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
    }

    @Test
    void parseNumericEdgeCasesTest() {
        // Tests negative numbers and large unsigned hex values (which fit in 64-bit 2's complement)
        String input = """
                .data
                neg_byte: .byte -1
                neg_quad: .quad -100
                large_hex: .quad 0xFFFFFFFFFFFFFFFF
                max_pos:   .quad 0x7FFFFFFFFFFFFFFF
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();
        long base = MemoryLayout.DATA_BASE;

        // .byte -1 -> 0xFF
        assertEquals((byte) -1, memory.readByte(base));

        // .quad -100
        assertEquals(-100L, memory.readQuad(base + 1));

        // .quad 0xFF...FF -> -1L
        assertEquals(-1L, memory.readQuad(base + 9));

        // .quad max positive
        assertEquals(Long.MAX_VALUE, memory.readQuad(base + 17));
    }

    @Test
    void parseDirectiveAliasesTest() {
        // .space and .skip are aliases for .zero logic
        String input = """
                .bss
                start:
                .space 4
                .skip 4
                end:
                """;

        Parser.parse(input, state, labelManager);
        long startAddr = labelManager.getDataLabel("start").address();
        long endAddr = labelManager.getDataLabel("end").address();

        // 4 + 4 = 8 bytes total
        assertEquals(startAddr + 8, endAddr);
    }

    @Test
    void parseMultipleArgsOnLineTest() {
        // Comma-separated values for various directives
        String input = """
                .data
                bytes: .byte 1, 2, 3
                quads: .quad 0x10, 0x20
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();
        long base = MemoryLayout.DATA_BASE;

        assertEquals((byte) 1, memory.readByte(base));
        assertEquals((byte) 2, memory.readByte(base + 1));
        assertEquals((byte) 3, memory.readByte(base + 2));

        // Quads start after the 3 bytes
        assertEquals(0x10L, memory.readQuad(base + 3));
        assertEquals(0x20L, memory.readQuad(base + 3 + 8));
    }

    // ==================== Complex Parsing Scenarios ====================

    @Test
    void parseComplexStringLiteralsTest() {
        // Tests strings containing characters that usually mean something else
        String input = """
                .data
                quote_in_str:   .ascii "Says \\"Hello\\""
                comma_in_str:   .ascii "A,B"
                hash_in_str:    .ascii "A # B"
                """;

        Parser.parse(input, state, labelManager);
        Memory memory = state.getMemory();
        long base = MemoryLayout.DATA_BASE;

        // "Says \"Hello\"" -> S a y s _ " H e l l o "
        // Length: 4 (Says) + 1 (space) + 1 (") + 5 (Hello) + 1 (") = 12 bytes
        // Let's spot check specific chars
        assertEquals('"', (char) memory.readByte(base + 5).byteValue());
        assertEquals('#', (char) memory.readByte(base + 12 + 3 + 2).byteValue()); // Rough offset check
    }

    @Test
    void parseSectionSwitchingTest() {
        // Switching back and forth between sections
        String input = """
                .text
                    movq val1, %rax
                .data
                    val1: .quad 10
                .text
                    addq $5, %rax
                .data
                    val2: .quad 20
                """;

        Parser.ParseResult result = Parser.parse(input, state, labelManager);

        // Check Instructions
        assertEquals(2, result.instructions().size());

        // Check Data
        assertTrue(labelManager.isDataLabel("val1"));
        assertTrue(labelManager.isDataLabel("val2"));

        // val2 should be immediately after val1 (8 bytes difference)
        assertEquals(
                labelManager.getDataLabel("val1").address() + 8,
                labelManager.getDataLabel("val2").address()
        );
    }

    @Test
    void parseAlignWhenAlreadyAlignedTest() {
        String input = """
                .data
                .quad 0x1111111111111111
                .align 8
                val: .quad 0x2222222222222222
                """;

        Parser.parse(input, state, labelManager);

        // First quad is 8 bytes. Address moves from 0x...000 to 0x...008.
        // 0x...008 is already 8-byte aligned. .align 8 should add 0 padding.
        long valAddr = labelManager.getDataLabel("val").address();
        assertEquals(MemoryLayout.DATA_BASE + 8, valAddr);
    }

    // ==================== Robustness & Edge Cases ====================

    @Test
    void parseEmptyOrCommentOnlyFileTest() {
        String input = """
                # Just a comment
                
                # Another comment
                """;
        Parser.ParseResult result = Parser.parse(input, state, labelManager);
        assertTrue(result.instructions().isEmpty());
    }

    @Test
    void parseImmediateLabelOrMemoryOperandTest() {
        // Integration test ensuring OperandParser works within Parser
        String input = """
                .text
                main:
                    movq $10, %rax       # Immediate
                    movq -8(%rbp), %rbx  # Memory
                    jmp main             # Label
                """;

        Parser.ParseResult result = Parser.parse(input, state, labelManager);
        assertEquals(3, result.instructions().size());
    }

    // ==================== Error Path Tests ====================

    @Test
    void parseInvalidDirectiveArgThrowsTest() {
        String input = """
                .data
                .byte NOT_A_NUMBER
                """;
        assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
    }

    @Test
    void parseInvalidLabelFormatThrowsTest() {
        // Labels cannot start with a digit
        String input = """
                .text
                1start:
                    nop
                """;
        assertThrows(IllegalArgumentException.class, () ->
                Parser.parse(input, state, labelManager)
        );
    }
}