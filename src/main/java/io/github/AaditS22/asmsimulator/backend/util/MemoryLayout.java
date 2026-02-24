package io.github.AaditS22.asmsimulator.backend.util;

public class MemoryLayout {
    // Defines helpful constants to aid with memory allocation
    // Bases for each section are not fully accurate to actual x86-64 assembly but an approximation for
    //      the sake of visualization
    public static final long CODE_BASE    = 0x400000L;
    public static final long READ_ONLY_DATA_BASE  = 0x600000L;
    public static final long DATA_BASE    = 0x800000L;
    public static final long BSS_BASE     = 0xA00000L;
    public static final long HEAP_BASE    = 0xC00000L;
    public static final long STACK_BASE   = 0x7FFFFFFFF000L;

    public static final long CODE_LIMIT           = READ_ONLY_DATA_BASE;
    public static final long READ_ONLY_DATA_LIMIT = DATA_BASE;
    public static final long DATA_LIMIT           = BSS_BASE;
    public static final long BSS_LIMIT            = HEAP_BASE;

    public static final int INSTRUCTION_SIZE = 8;
}
