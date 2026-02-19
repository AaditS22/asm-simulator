package io.github.AaditS22.asmsimulator.backend.cpu;

public class IOBuffer {
    private final StringBuilder buffer = new StringBuilder();

    /**
     * Adds text to the buffer
     * @param text The text to add
     */
    public void append(String text) {
        buffer.append(text);
    }

    /**
     * Flushes the buffer and returns the contents of it
     * @return The contents of the buffer
     */
    public String flush() {
        String result = buffer.toString();
        buffer.setLength(0);
        return result;
    }

    /**
     * Resets the buffer
     */
    public void reset() {
        buffer.setLength(0);
    }

    /**
     * Checks if the buffer is empty
     * @return true if the buffer is empty, false otherwise
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}