package io.github.AaditS22.asmsimulator.backend.util;

public record StepResult(String description, String output) {
    /**
     * Checks if the output is empty or not
     * @return false if the output is empty, true otherwise
     */
    public boolean hasOutput() {
        return !output.isEmpty();
    }
}