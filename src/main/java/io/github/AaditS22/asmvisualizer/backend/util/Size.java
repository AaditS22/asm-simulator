package io.github.AaditS22.asmvisualizer.backend.util;

public enum Size {
    BYTE(1),
    WORD(2),
    LONG(4),
    QUAD(8);

    private final int bytes;

    Size(int bytes) {
        this.bytes = bytes;
    }

    /**
     * Helper method to get a Size enum based on the character on an instruction (e.g. mov(q))
     * @param c the character to parse
     * @return a Size enum that represents the character
     */
    public static Size getSize(char c) {
        return switch (c) {
            case 'b' -> BYTE;
            case 'w' -> WORD;
            case 'l' -> LONG;
            case 'q' -> QUAD;
            default -> null;
        };
    }

    /**
     * Gets the number of bytes in the enum
     * @return the number of bytes
     */
    public int getBytes() {
        return bytes;
    }
}
