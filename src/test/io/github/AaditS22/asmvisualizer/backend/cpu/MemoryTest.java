package io.github.AaditS22.asmvisualizer.backend.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTest {
    private Memory memory;

    @BeforeEach
    void setUp() {
        Map<String, Long> registers = new HashMap<>();
        long rsp = 0x7FFFFFFFF000L;
        registers.put("rsp", rsp - 72);
        memory = new Memory(registers);
        for (int i = 0; i < 10; i++) {
            memory.writeQuad(rsp - (i * 8), 10 * i);
        }
    }

    @Test
    void byteReadWriteTest() {
        memory.writeByte(0x1000L, (byte) 25);
        assertEquals((byte) 25, memory.readByte(0x1000L));
    }

    @Test
    void wordReadWriteTest() {
        memory.writeWord(0x1000L, (short) 0xABCD);
        assertEquals((short) 0xABCD, memory.readWord(0x1000L));
        assertEquals((byte) 0xCD, memory.readByte(0x1000L));
        assertEquals((byte) 0xAB, memory.readByte(0x1000L + 1));
    }

    @Test
    void longReadWriteTest() {
        memory.writeLong(0x1000L, 0xABCDABCB);
        assertEquals(0xABCDABCB, memory.readLong(0x1000L));
    }

    @Test
    void quadReadWriteTest() {
        memory.writeQuad(0x1000L, 0xABCDABCBABCDABCBL);
        assertEquals(0xABCDABCBABCDABCBL, memory.readQuad(0x1000L));
    }

    @Test
    void getStackTest() {
        Long[] expected = {0L, 10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L};
        assertArrayEquals(expected, memory.getStack());
        memory.setStackViewStart(memory.getStackViewStart() - 8);
        expected = new Long[]{10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 0L};
        assertArrayEquals(expected, memory.getStack());
    }

    @Test
    void pushStackTest() {
        memory.setStackViewStart(memory.getStackViewStart() - 8);
        Long[] expected = new Long[]{10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L};
        assertArrayEquals(expected, memory.stackPush(100L));
    }

    @Test
    void popStackTest() {
        assertEquals(90L,  memory.stackPop());
        assertEquals(80L,  memory.stackPop());
    }
}