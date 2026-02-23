package io.github.AaditS22.asmsimulator.backend.cpu;

import io.github.AaditS22.asmsimulator.backend.util.DataLabel;

import java.util.HashMap;
import java.util.Map;

public class LabelManager {
    private final Map<String, Long> codeLabels;
    private final Map<String, DataLabel> dataLabels;

    public LabelManager() {
        codeLabels = new HashMap<>();
        dataLabels = new HashMap<>();
    }

    // Helper methods

    /**
     * Checks if a label is a codeLabel
     * @param name the name of the label to check
     * @return a boolean for whether it is a codeLabel or not
     */
    public boolean isCodeLabel(String name) {
        return codeLabels.containsKey(name);
    }

    /**
     * Checks if a label is a dataLabel
     * @param name the name of the label to check
     * @return a boolean for whether it is a dataLabel or not
     */
    public boolean isDataLabel(String name) {
        return dataLabels.containsKey(name);
    }

    /**
     * Checks if the program has a label with a given name
     * @param name the name of the label to test
     * @return a boolean for whether the label exists or not
     */
    public boolean hasLabel(String name) {
        return isCodeLabel(name) || isDataLabel(name);
    }

    /**
     * Adds a new codeLabel if it does not already exist
     * @param name the name of the label
     * @param instructionAddress the address of the instruction it corresponds to
     */
    public void addCodeLabel(String name, long instructionAddress) {
        if (hasLabel(name)) {
            throw new IllegalArgumentException("The label: " + name + " already exists!");
        }
        codeLabels.put(name, instructionAddress);
    }

    /**
     * Creates a new dataLabel record and adds it to the map
     * @param name the name of the label
     * @param address the address of the label
     * @param size the size of the stored value
     */
    public void addDataLabel(String name, long address, int size) {
        if (hasLabel(name)) {
            throw new IllegalArgumentException("The label: " + name + " already exists!");
        }
        dataLabels.put(name, new DataLabel(address, size));
    }

    /**
     * Gets the value stored at a code label if it exists
     * @param name the name of the label
     * @return a long corresponding to the address of the instruction at the label
     */
    public long getCodeLabel(String name) {
        if (!isCodeLabel(name)) {
            throw new IllegalArgumentException("The label '" + name + "' does not exist!");
        }
        return codeLabels.get(name);
    }

    /**
     * Gets the value stored at a data label if it exists
     * @param name the name of the label
     * @return a DataLabel object, with the address of the label and its size
     */
    public DataLabel getDataLabel(String name) {
        if (!isDataLabel(name)) {
            throw new IllegalArgumentException("The label: " + name + " does not exist!");
        }
        return dataLabels.get(name);
    }

    /**
     * Gets all data labels mapped by their name.
     * @return a map of data label names to DataLabel objects
     */
    public Map<String, DataLabel> getDataLabels() {
        return dataLabels;
    }

    /**
     * Resets the label manager by clearing all stored labels.
     */
    public void reset() {
        codeLabels.clear();
        dataLabels.clear();
    }
}
