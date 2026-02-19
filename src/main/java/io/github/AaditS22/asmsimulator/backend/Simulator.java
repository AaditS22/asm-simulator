package io.github.AaditS22.asmsimulator.backend;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.input.Parser;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Simulator {
    // Helper enum to represent the current status of the simulator
    // IDLE: No program loaded yet
    // READY: Program loaded, ready to execute
    // RUNNING: Program is currently executing
    // HALTED: Program has halted execution
    public enum Status { IDLE, READY, RUNNING, HALTED }

    private final CPUState state;
    private final LabelManager labelManager;

    private List<Instruction> instructions = List.of();
    private String lastSource = null;
    private String entryPoint = "_start";
    private Status status = Status.IDLE;

    /**
     * Initializes a new Simulator object with CPUState and LabelManager objects
     */
    public Simulator() {
        state = new CPUState();
        labelManager = new LabelManager();
    }

    /**
     * Loads a new program into the simulator
     * @param source the source code of the program to load
     */
    public void load(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Source code cannot be empty.");
        }

        state.restartProgram();

        Parser.ParseResult result = Parser.parse(source, state, labelManager);

        this.instructions = result.instructions();
        this.entryPoint = result.entryPoint();
        this.lastSource = source;

        jumpToEntryPoint();

        this.status = instructions.isEmpty() ? Status.HALTED : Status.READY;
    }

    /**
     * Performs the next step of execution
     * @return the description of the executed instruction
     */
    public String step() {
        if (status == Status.IDLE) {
            throw new IllegalStateException("No program loaded. Call load() first.");
        }
        if (status == Status.HALTED) {
            throw new IllegalStateException("Program has already halted.");
        }

        long pc = state.getPC();
        int index = pcToIndex(pc);

        if (index < 0 || index >= instructions.size()) {
            throw new IllegalStateException(
                    "PC 0x" + Long.toHexString(pc).toUpperCase() + " is outside the code section.");
        }

        status = Status.RUNNING;
        Instruction current = instructions.get(index);

        String description;
        try {
            description = current.getDescription(state, labelManager);
        } catch (Exception e) {
            description = current.getMnemonic() + " (description unavailable)";
        }

        current.execute(state, labelManager);

        long newPc = state.getPC();
        int newIndex = pcToIndex(newPc);

        if (newIndex < 0) {
            throw new IllegalStateException(
                    "PC jumped to invalid address 0x" + Long.toHexString(newPc).toUpperCase() + ".");
        }

        status = newIndex >= instructions.size() ? Status.HALTED : Status.READY;
        return description;
    }

    /**
     * Runs the program until it halts or reaches the maximum number of steps
     * @param maxSteps the maximum number of steps to run
     * @return a list of descriptions of the executed instructions
     */
    public List<String> runAll(int maxSteps) {
        if (status == Status.IDLE) {
            throw new IllegalStateException("No program loaded. Call load() first.");
        }
        if (status == Status.HALTED) {
            throw new IllegalStateException("Program has already halted.");
        }

        List<String> descriptions = new ArrayList<>();
        for (int i = 0; i < maxSteps; i++) {
            descriptions.add(step());
            if (status == Status.HALTED) {
                break;
            }
        }

        if (status != Status.HALTED && descriptions.size() == maxSteps) {
            throw new IllegalStateException(
                    "Execution limit of " + maxSteps + " steps reached. Possible infinite loop.");
        }

        return Collections.unmodifiableList(descriptions);
    }

    /**
     * Resets the simulator to its initial state
     */
    public void reset() {
        if (lastSource == null) {
            throw new IllegalStateException("No program has been loaded yet.");
        }
        load(lastSource);
    }

    /**
     * Gets the current state of the CPU
     * @return the current state of the CPU
     */
    public CPUState getState() {
        return state;
    }

    /**
     * Gets the LabelManager object associated with this Simulator
     * @return the LabelManager object associated with this Simulator
     */
    public LabelManager getLabelManager() {
        return labelManager;
    }

    /**
     * Gets the list of instructions loaded into the simulator
     * @return the list of instructions loaded into the simulator
     */
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Gets the current status of the simulator
     * @return the current status of the simulator
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Checks if the simulator is halted
     * @return true if the simulator is halted, false otherwise
     */
    public boolean isHalted() {
        return status == Status.HALTED;
    }

    /**
     * Checks if the simulator is ready to execute the next instruction
     * @return true if the simulator is ready, false otherwise
     */
    public boolean isReady() {
        return status == Status.READY;
    }

    /**
     * Gets the index of the current instruction
     * @return the index of the current instruction
     */
    public int getCurrentInstructionIndex() {
        return pcToIndex(state.getPC());
    }

    /**
     * Gets the current instruction being executed
     * @return the current instruction being executed, or null if the PC is outside the code section
     */
    public Instruction getCurrentInstruction() {
        int index = getCurrentInstructionIndex();
        if (index < 0 || index >= instructions.size()) {
            return null;
        }
        return instructions.get(index);
    }

    /**
     * Gets the total number of instructions loaded into the simulator
     * @return the total number of instructions loaded into the simulator
     */
    public int getTotalInstructions() {
        return instructions.size();
    }

    /**
     * Converts a program counter value to an index in the instruction list
     * @param pc the value of the program counter
     * @return the index of the instruction at the given program counter
     */
    private int pcToIndex(long pc) {
        return (int) ((pc - MemoryLayout.CODE_BASE) / MemoryLayout.INSTRUCTION_SIZE);
    }

    /**
     * Jumps the program counter to the entry point of the program
     */
    private void jumpToEntryPoint() {
        if (labelManager.isCodeLabel(entryPoint)) {
            state.setPC(labelManager.getCodeLabel(entryPoint));
        } else {
            state.setPC(MemoryLayout.CODE_BASE);
        }
    }
}