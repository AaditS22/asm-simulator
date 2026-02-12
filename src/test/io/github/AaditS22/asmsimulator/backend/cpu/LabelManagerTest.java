package io.github.AaditS22.asmsimulator.backend.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelManagerTest {
    private LabelManager labelManager;

    @BeforeEach
    void setUp() {
        labelManager = new LabelManager();
    }

    @Test
    void addDuplicateTest() {
        labelManager.addCodeLabel("test", 0);
        assertThrows(IllegalArgumentException.class, () ->
                labelManager.addCodeLabel("test", 0));

        labelManager.addDataLabel("test2", 0x100L, 3);
        assertThrows(IllegalArgumentException.class, () ->
                labelManager.addDataLabel("test2", 0x100L, 3));
    }

    @Test
    void checkLabelTypeTest() {
        labelManager.addCodeLabel("test", 0);
        assertTrue(labelManager.isCodeLabel("test"));
        assertFalse(labelManager.isDataLabel("test"));

        labelManager.addDataLabel("test2", 0x100L, 3);
        assertTrue(labelManager.isDataLabel("test2"));
        assertFalse(labelManager.isCodeLabel("test2"));
    }

    @Test
    void getLabelDoesNotExistTest() {
        labelManager.addCodeLabel("test", 0);
        assertThrows(IllegalArgumentException.class, () ->
                labelManager.getCodeLabel("test2"));
        assertThrows(IllegalArgumentException.class, () ->
                labelManager.getDataLabel("test"));

        labelManager.addDataLabel("test2", 0x100L, 3);
        assertThrows(IllegalArgumentException.class, () ->
                labelManager.getCodeLabel("test2"));
    }

    @Test
    void getLabelTest() {
        labelManager.addCodeLabel("test", 0);
        assertEquals(0, labelManager.getCodeLabel("test"));

        labelManager.addDataLabel("test2", 0x100L, 3);
        assertEquals(0x100L, labelManager.getDataLabel("test2").address());
    }
}