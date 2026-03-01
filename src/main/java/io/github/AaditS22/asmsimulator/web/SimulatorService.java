package io.github.AaditS22.asmsimulator.web;

import io.github.AaditS22.asmsimulator.backend.Simulator;
import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.util.DataLabel;
import io.github.AaditS22.asmsimulator.backend.util.StepResult;
import io.github.AaditS22.asmsimulator.web.dto.LoadResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.RunResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.StateDto;
import io.github.AaditS22.asmsimulator.web.dto.StepResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// DISCLAIMER: Many of the utility methods in this class were written with the help of LLMs
@Service
public class SimulatorService {

    private static final int RUN_STEP_LIMIT = 100_000;
    private static final int STACK_ROWS = 10;

    private final Simulator simulator = new Simulator();
    private boolean loaded = false;
    private String currentCode = "";
    private List<Integer> instructionLineMap = new ArrayList<>();
    private int stepCount = 0;

    private Map<Long, Long> lastMemoryValues = new LinkedHashMap<>();
    private final Set<Long> manualTrackedAddresses = new TreeSet<>();

    /**
     * Loads the given code into the simulator and returns a response DTO.
     * @param code The code to load
     * @return A LoadResponseDto indicating success or failure
     */
    public synchronized LoadResponseDto load(String code) {
        try {
            simulator.load(code);
            loaded = true;
            currentCode = code;
            stepCount = 0;
            instructionLineMap = buildInstructionLineMap(code);
            lastMemoryValues.clear();
            manualTrackedAddresses.clear();
            return new LoadResponseDto(true, null, buildState(false));
        } catch (Exception e) {
            loaded = false;
            return new LoadResponseDto(false, e.getMessage(), null);
        }
    }

    /**
     * Runs a single step and returns a StepResponseDto.
     * @return A StepResponseDto containing the result of the step and the updated state
     */
    public synchronized StepResponseDto step() {
        if (!loaded) {
            return new StepResponseDto(null, null, "", false, null, "No program loaded");
        }
        if (simulator.isHalted()) {
            return new StepResponseDto(null, null, "", false, buildState(false), "Program has halted");
        }
        if (simulator.isWaitingForInput()) {
            return new StepResponseDto(null, null, "", false, buildState(false), "Waiting for input");
        }
        try {
            Instruction currentInst = simulator.getCurrentInstruction();
            String mnemonic = currentInst != null ? currentInst.getMnemonic() : "";

            StepResult result = simulator.step();
            stepCount++;

            autoScrollStack();
            StateDto state = buildState(true);
            return new StepResponseDto(result.description(), mnemonic, result.output(),
                    result.hasOutput(), state, null);
        } catch (Exception e) {
            return new StepResponseDto(null, null, "", false, buildState(false), e.getMessage());
        }
    }

    /**
     * Runs the program until it halts or reaches the step limit.
     * @return A RunResponseDto containing the final state and output
     */
    public synchronized RunResponseDto runToEnd() {
        if (!loaded) {
            return new RunResponseDto("", null, null, 0, null, "No program loaded");
        }
        if (simulator.isHalted()) {
            return new RunResponseDto("", null, null, simulator.getExitCode(), buildState(false),
                    "Program has already halted");
        }

        StringBuilder outputBuf = new StringBuilder();
        String lastDesc = "";
        String lastMnemonic = "";
        int steps = 0;

        try {
            while (!simulator.isHalted() && !simulator.isWaitingForInput() && steps < RUN_STEP_LIMIT) {
                Instruction currentInst = simulator.getCurrentInstruction();
                if (currentInst != null) lastMnemonic = currentInst.getMnemonic();

                StepResult result = simulator.step();
                stepCount++;
                steps++;

                if (result.hasOutput()) outputBuf.append(result.output());
                if (result.description() != null) lastDesc = result.description();
            }

            autoScrollStack();
            long exitCode = simulator.isHalted() ? simulator.getExitCode() : 0;
            return new RunResponseDto(outputBuf.toString(), lastDesc, lastMnemonic,
                    exitCode, buildState(true), null);
        } catch (Exception e) {
            return new RunResponseDto(outputBuf.toString(), lastDesc, lastMnemonic,
                    0, buildState(false), e.getMessage());
        }
    }

    /**
     * Resets the simulator and returns the current state.
     * @return The current state after resetting
     */
    public synchronized StateDto reset() {
        if (!loaded) return buildEmptyState();
        try {
            simulator.reset();
            stepCount = 0;
            instructionLineMap = buildInstructionLineMap(currentCode);
            lastMemoryValues.clear();
            manualTrackedAddresses.clear();
            return buildState(false);
        } catch (Exception e) {
            return buildState(false);
        }
    }

    /**
     * Provides input to the simulator and returns the result of the step.
     * @param value The input value to provide
     * @return A StepResponseDto containing the result of the step and the updated state
     */
    public synchronized StepResponseDto provideInput(String value) {
        if (!simulator.isWaitingForInput()) {
            return new StepResponseDto(null, null, "", false, buildState(false),
                    "Not waiting for input");
        }
        try {
            simulator.provideInput(value);
            return step();
        } catch (Exception e) {
            return new StepResponseDto(null, null, "", false, buildState(false), e.getMessage());
        }
    }

    /**
     * Gets the current state of the simulator.
     * @return The current state of the simulator
     */
    public synchronized StateDto getState() {
        if (!loaded) return buildEmptyState();
        return buildState(false);
    }

    /**
     * Scrolls the stack by the given direction.
     * @param direction 1 for up, -1 for down
     * @return The updated state after scrolling
     */
    public synchronized StateDto scrollStack(int direction) {
        if (!loaded) return buildEmptyState();
        Memory mem = simulator.getState().getMemory();
        mem.setStackViewStart(mem.getStackViewStart() + direction * 8L);
        return buildState(false);
    }

    /**
     * Tracks a memory address for manual inspection.
     * @param addressStr The memory address to track, in hexadecimal/decimal format
     * @return The updated state after tracking the address
     */
    public synchronized StateDto trackMemoryAddress(String addressStr) {
        if (!loaded) return buildEmptyState();
        try {
            long addr = addressStr.startsWith("0x")
                    ? Long.parseLong(addressStr.substring(2), 16)
                    : Long.parseLong(addressStr);
            manualTrackedAddresses.add(addr);
            return buildState(false);
        } catch (NumberFormatException e) {
            return buildState(false);
        }
    }

    private void autoScrollStack() {
        if (!loaded) return;
        CPUState cpu = simulator.getState();
        Memory mem = cpu.getMemory();
        long rsp = cpu.getRegister("rsp", 8);
        long viewStart = mem.getStackViewStart();
        long viewEnd = viewStart - (long)(STACK_ROWS - 1) * 8;

        if (rsp > viewStart) {
            mem.setStackViewStart(rsp);
        } else if (rsp < viewEnd) {
            mem.setStackViewStart(rsp + (long)(STACK_ROWS - 1) * 8);
        }
    }

    private StateDto buildState(boolean detectChanges) {
        if (!loaded) return buildEmptyState();

        CPUState cpu = simulator.getState();
        LabelManager lm = simulator.getLabelManager();
        Memory mem = cpu.getMemory();

        String status = resolveStatus();
        int instIndex = simulator.getCurrentInstructionIndex();
        int lineNumber = resolveLineNumber(instIndex);

        Map<String, Long> registers = buildRegisters(cpu);
        StateDto.FlagsDto flags = buildFlags(cpu);
        List<StateDto.StackRowDto> stack = buildStack(cpu);
        List<StateDto.MemoryEntryDto> memory = buildMemory(cpu, lm, detectChanges);

        return new StateDto(status, stepCount, instIndex, lineNumber,
                instructionLineMap, registers, flags, stack, memory);
    }

    private StateDto buildEmptyState() {
        return new StateDto("NOT_LOADED", 0, -1, -1,
                List.of(), Map.of(), new StateDto.FlagsDto(false, false, false, false),
                List.of(), List.of());
    }

    private String resolveStatus() {
        if (simulator.isHalted()) return "HALTED";
        if (simulator.isWaitingForInput()) return "WAITING_FOR_INPUT";
        return simulator.getStatus().name();
    }

    private int resolveLineNumber(int instIndex) {
        if (instIndex < 0 || instIndex >= instructionLineMap.size()) return -1;
        return instructionLineMap.get(instIndex);
    }

    private Map<String, Long> buildRegisters(CPUState cpu) {
        Map<String, Long> regs = new LinkedHashMap<>();
        for (String name : List.of("rax", "rbx", "rcx", "rdx", "rsi", "rdi",
                "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15", "rsp", "rbp", "rip")) {
            regs.put(name, cpu.getRegister(name, 8));
        }
        return regs;
    }

    private StateDto.FlagsDto buildFlags(CPUState cpu) {
        return new StateDto.FlagsDto(
                cpu.getFlags().isZero(),
                cpu.getFlags().isCarry(),
                cpu.getFlags().isNegative(),
                cpu.getFlags().isOverflow()
        );
    }

    private List<StateDto.StackRowDto> buildStack(CPUState cpu) {
        Memory mem = cpu.getMemory();
        Long[] values = mem.getStack();
        long viewStart = mem.getStackViewStart();
        long rsp = cpu.getRegister("rsp", 8);
        long rbp = cpu.getRegister("rbp", 8);

        List<StateDto.StackRowDto> rows = new ArrayList<>();
        int rowCount = values.length;
        for (int di = 0; di < rowCount; di++) {
            long addr = viewStart - (long) di * 8;
            long val = values[di];
            rows.add(new StateDto.StackRowDto(
                    toHex(addr), addr,
                    val, toHex(val),
                    addr == rsp, addr == rbp
            ));
        }
        return rows;
    }

    private List<StateDto.MemoryEntryDto> buildMemory(CPUState cpu, LabelManager lm, boolean detectChanges) {
        Memory mem = cpu.getMemory();
        Map<Long, String> labelNames = new LinkedHashMap<>();
        Map<Long, Integer> labelSizes = new LinkedHashMap<>();
        Map<Long, String> trackedReasons = mem.getTrackedReasons();

        Set<Long> addresses = new TreeSet<>();

        if (lm != null) {
            for (Map.Entry<String, DataLabel> entry : lm.getDataLabels().entrySet()) {
                long addr = entry.getValue().address();
                addresses.add(addr);
                labelNames.put(addr, entry.getKey());
                labelSizes.put(addr, entry.getValue().size());
            }
        }

        addresses.addAll(mem.getAccessedAddresses());
        addresses.addAll(manualTrackedAddresses);

        List<StateDto.MemoryEntryDto> entries = new ArrayList<>();
        for (Long addr : addresses) {
            int size = labelSizes.getOrDefault(addr, 8);
            long val = mem.readN(addr, size);
            boolean changed = false;

            if (detectChanges) {
                Long prev = lastMemoryValues.get(addr);
                changed = (prev != null && prev != val);
            }
            lastMemoryValues.put(addr, val);

            entries.add(new StateDto.MemoryEntryDto(
                    toHex(addr), addr,
                    labelNames.get(addr),
                    trackedReasons.get(addr),
                    val, toHex(val),
                    size, changed
            ));
        }
        return entries;
    }

    private String toHex(long v) {
        if (v == 0) return "0x0";
        return "0x" + Long.toHexString(v).toUpperCase();
    }

    private List<Integer> buildInstructionLineMap(String code) {
        List<Integer> map = new ArrayList<>();
        String[] lines = code.split("\\R", -1);
        boolean inText = true;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int commentIdx = line.indexOf('#');
            if (commentIdx >= 0) line = line.substring(0, commentIdx);
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String possibleLabel = line.substring(0, colonIdx).trim();
                if (!possibleLabel.contains(" ") && !possibleLabel.contains("\t")) {
                    line = line.substring(colonIdx + 1).trim();
                }
            }
            if (line.isEmpty()) continue;

            if (line.equals(".text")) { inText = true; continue; }
            if (line.equals(".data") || line.equals(".bss") || line.equals(".rodata")) {
                inText = false; continue;
            }
            if (line.startsWith(".section")) {
                inText = line.contains("text");
                continue;
            }
            if (line.startsWith(".")) continue;

            if (inText) {
                map.add(i);
            }
        }
        return map;
    }
}