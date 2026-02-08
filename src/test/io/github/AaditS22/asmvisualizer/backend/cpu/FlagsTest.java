package io.github.AaditS22.asmvisualizer.backend.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlagsTest {
    Flags flags;

    @BeforeEach
    void setUp() {
        flags = new Flags();
    }

    @Test
    void addOperationTest() {
        flags.updateAddFlags(255L, 1L, 8);
        assertTrue(flags.isZero());
        assertFalse(flags.isNegative());
        assertTrue(flags.isCarry());
        assertFalse(flags.isOverflow());

        flags.updateAddFlags(127L, 1L, 8);
        assertFalse(flags.isZero());
        assertTrue(flags.isNegative());
        assertFalse(flags.isCarry());
        assertTrue(flags.isOverflow());

        flags.updateAddFlags(0xFFFFFFFFFFFFFFFFL, 1L, 64);
        assertTrue(flags.isZero());
        assertFalse(flags.isNegative());
        assertTrue(flags.isCarry());
        assertFalse(flags.isOverflow());
    }

    @Test
    void subOperationTest() {
        flags.updateSubFlags(10L, 20L, 8);
        assertFalse(flags.isZero());
        assertTrue(flags.isNegative());
        assertTrue(flags.isCarry());
        assertFalse(flags.isOverflow());

        flags.updateSubFlags(0x7FFFFFFFFFFFFFFFL, -1L, 64);
        assertFalse(flags.isZero());
        assertTrue(flags.isNegative());
        assertTrue(flags.isCarry());
        assertTrue(flags.isOverflow());
    }

    @Test
    void logicalOperationTest() {
        flags.updateLogicalFlags(0x8000000000000000L, 64);
        assertTrue(flags.isNegative());
        assertFalse(flags.isZero());
        assertFalse(flags.isCarry());
        assertFalse(flags.isOverflow());

        flags.updateLogicalFlags(0L, 64);
        assertTrue(flags.isZero());
        assertFalse(flags.isNegative());
    }

    @Test
    void updateIncrementTest() {
        flags.updateIncFlags(0x7FFFFFFFFFFFFFFFL, 64);
        assertTrue(flags.isNegative());
        assertTrue(flags.isOverflow());

        flags.updateAddFlags(0xFFFFFFFFFFFFFFFFL, 1, 64);
        assertTrue(flags.isCarry());
        flags.updateIncFlags(10L, 64);
        assertTrue(flags.isCarry());
    }

    @Test
    void updateDecrementTest() {
        flags.updateDecFlags(0L, 64);
        assertTrue(flags.isNegative());
        assertFalse(flags.isZero());

        flags.updateDecFlags(0x8000000000000000L, 64);
        assertFalse(flags.isNegative());
        assertTrue(flags.isOverflow());
    }

    @Test
    void updateNegateTest() {
        flags.updateNegateFlags(0L, 64);
        assertTrue(flags.isZero());
        assertFalse(flags.isCarry());

        flags.updateNegateFlags(1L, 64);
        assertTrue(flags.isNegative());
        assertTrue(flags.isCarry());

        flags.updateNegateFlags(0x8000000000000000L, 64);
        assertTrue(flags.isOverflow());
        assertTrue(flags.isNegative());
        assertTrue(flags.isCarry());
    }
}