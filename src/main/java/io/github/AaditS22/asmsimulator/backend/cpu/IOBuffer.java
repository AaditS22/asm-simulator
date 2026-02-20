package io.github.AaditS22.asmsimulator.backend.cpu;

public class IOBuffer {
    private final StringBuilder buffer = new StringBuilder();
    private String pendingInput = null;
    private boolean waitingForInput = false;
    private boolean exitRequested = false;
    private long exitCode = 0;

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
        pendingInput = null;
        waitingForInput = false;
        exitRequested = false;
        exitCode = 0;
    }

    /**
     * Checks if the buffer is empty
     * @return true if the buffer is empty, false otherwise
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Sets the pending input to the given string
     * @param input The input to set
     */
    public void setInput(String input) {
        this.pendingInput = input;
        this.waitingForInput = false;
    }

    /**
     * Checks if there is pending input
     * @return true if there is pending input, false otherwise
     */
    public boolean hasInput() {
        return pendingInput != null;
    }

    /**
     * Consumes the pending input and returns it
     * @return The consumed input
     */
    public String consumeInput() {
        String input = pendingInput;
        pendingInput = null;
        return input;
    }

    /**
     * Checks if the CPU is waiting for input
     * @return true if the CPU is waiting for input, false otherwise
     */
    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    /**
     * Requests the CPU to exit with the given code
     * @param code The exit code to use
     */
    public void requestExit(long code) {
        this.exitRequested = true;
        this.exitCode = code;
    }

    /**
     * Checks if the CPU has requested to exit
     * @return true if the CPU has requested to exit, false otherwise
     */
    public boolean isExitRequested() {
        return exitRequested;
    }

    /**
     * Gets the exit code to use
     * @return The exit code to use
     */
    public long getExitCode() {
        return exitCode;
    }

    /**
     * Sets the waiting flag
     * @param waiting whether to set it to true or false
     */
    public void setWaitingForInput(boolean waiting) {
        this.waitingForInput = waiting;
    }
}