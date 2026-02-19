package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.MemoryLayout;

import java.util.ArrayList;
import java.util.List;

// DISCLAIMER: Due to the complexity of input parsing, much of this class was written with the help of LLMs
public class Parser {

    // Record to hold the result of the parsing process
    public record ParseResult(List<Instruction> instructions, LabelManager labelManager, String entryPoint) {}

    // Inner record to keep track of line numbers during processing
    private record Line(int lineNumber, String content) {}

    private enum Section {
        TEXT, DATA, BSS, RODATA
    }

    /**
     * Main method to parse user input
     * @param input The raw assembly source code.
     * @param state The CPU state initially
     * @param labelManager The label manager of the CPU state initially
     * @return A ParseResult containing parsed instructions and metadata
     */
    public static ParseResult parse(String input, CPUState state, LabelManager labelManager) {
        labelManager.reset();
        List<Line> lines = preprocess(input);

        String entryPoint = firstPass(lines, labelManager);

        List<Instruction> instructions = secondPass(lines, labelManager, state.getMemory());

        return new ParseResult(instructions, labelManager, entryPoint);
    }

    /**
     * First pass of the parsing process, collecting labels and defining the main entry point
     * @param lines the lines of assembly code
     * @param labelManager the label manager to add labels to
     * @return the name of the entry point
     */
    private static String firstPass(List<Line> lines, LabelManager labelManager) {
        long textAddr = MemoryLayout.CODE_BASE;
        long dataAddr = MemoryLayout.DATA_BASE;
        long rodataAddr = MemoryLayout.READ_ONLY_DATA_BASE;
        long bssAddr = MemoryLayout.BSS_BASE;
        Section currentSection = Section.TEXT;
        String entryPoint = "_start";
        List<String> pendingDataLabels = new ArrayList<>();

        for (Line lineObj : lines) {
            String line = lineObj.content; int lineNum = lineObj.lineNumber;
            try {
                String potentialLabel;
                int colonIndex = findColonOutsideQuotes(line);
                if (colonIndex != -1) {
                    potentialLabel = line.substring(0, colonIndex).trim();
                    if (isValidLabel(potentialLabel)) {
                        if (currentSection == Section.TEXT) {
                            labelManager.addCodeLabel(potentialLabel, textAddr);
                        } else {
                            pendingDataLabels.add(potentialLabel);
                        }
                        line = line.substring(colonIndex + 1).trim();
                    } else {
                        throw new IllegalArgumentException(
                                "Invalid label name: '" + potentialLabel
                                        + "'. Labels must start with a letter or underscore and contain no spaces.");
                    }
                }
                if (line.startsWith(".section") || isSectionDirective(line)) {
                    flushPendingLabels(pendingDataLabels, labelManager,
                            getCurrentAddress(currentSection, textAddr, dataAddr, rodataAddr, bssAddr), 0);
                    currentSection = parseSectionDirective(line);
                    continue;
                }
                if (line.startsWith(".globl") || line.startsWith(".global")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        entryPoint = parts[1].trim();
                    }
                    continue;
                }
                if (line.isEmpty()) continue;
                if (line.startsWith(".")) {
                    long currentAddr = getCurrentAddress(currentSection, textAddr, dataAddr, rodataAddr, bssAddr);
                    int size = computeDirectiveSize(line, currentAddr);
                    flushPendingLabels(pendingDataLabels, labelManager, currentAddr, size);
                    switch (currentSection) {
                        case TEXT -> textAddr += size;
                        case DATA -> dataAddr += size;
                        case RODATA -> rodataAddr += size;
                        case BSS -> bssAddr += size;}
                }
                else {
                    if (currentSection != Section.TEXT) {
                        throw new IllegalArgumentException("Instructions are not allowed in the "
                                + currentSection + " section");
                    }
                    textAddr += MemoryLayout.INSTRUCTION_SIZE;}
                validateOverflow(currentSection, textAddr, dataAddr, rodataAddr, bssAddr);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error on line " + lineNum + ": " + e.getMessage(), e);
            }
        }
        flushPendingLabels(pendingDataLabels, labelManager,
                getCurrentAddress(currentSection, textAddr, dataAddr, rodataAddr, bssAddr), 0);
        return entryPoint;
    }

    private static List<Instruction> secondPass(List<Line> lines, LabelManager labelManager, Memory memory) {
        List<Instruction> instructions = new ArrayList<>();
        long textAddr = MemoryLayout.CODE_BASE;
        long dataAddr = MemoryLayout.DATA_BASE;
        long rodataAddr = MemoryLayout.READ_ONLY_DATA_BASE;
        long bssAddr = MemoryLayout.BSS_BASE;
        Section currentSection = Section.TEXT;

        for (Line lineObj : lines) {
            String line = lineObj.content;
            int lineNum = lineObj.lineNumber;
            try {
                int colonIndex = findColonOutsideQuotes(line);
                if (colonIndex != -1) {
                    line = line.substring(colonIndex + 1).trim();
                }
                if (line.isEmpty()) continue;
                if (line.startsWith(".")) {
                    if (line.startsWith(".section") || isSectionDirective(line)) {
                        currentSection = parseSectionDirective(line);
                        continue;
                    }
                    if (line.startsWith(".globl") || line.startsWith(".global")) {
                        continue;
                    }
                    long currentAddr = getCurrentAddress(currentSection, textAddr, dataAddr, rodataAddr, bssAddr);
                    long advancedAddr = writeDirective(line, currentAddr, memory);

                    long increment = advancedAddr - currentAddr;
                    switch (currentSection) {
                        case TEXT -> textAddr += increment;
                        case DATA -> dataAddr += increment;
                        case RODATA -> rodataAddr += increment;
                        case BSS -> bssAddr += increment;
                    }
                }
                else {
                    if (currentSection != Section.TEXT) {
                        throw new IllegalArgumentException("Instructions are not allowed in the "
                                + currentSection + " section");
                    }
                    String[] parts = line.split("\\s+", 2);
                    String mnemonic = parts[0];
                    String operandString = parts.length > 1 ? parts[1] : "";

                    boolean isBranch = InstructionCreator.isBranchMnemonic(mnemonic);
                    List<Operand> operands = OperandParser.parseAll(operandString, isBranch);

                    Instruction instruction = InstructionCreator.create(mnemonic, operands);
                    instructions.add(instruction);

                    textAddr += MemoryLayout.INSTRUCTION_SIZE;
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("Error on line " + lineNum + ": " + e.getMessage(), e);
            }
        }
        return instructions;
    }

    /**
     * Helper method to preprocess the raw input into assembly lines (ignoring comments)
     * @param input the raw input
     * @return a list of assembly lines
     */
    private static List<Line> preprocess(String input) {
        List<Line> result = new ArrayList<>();
        if (input == null) return result;

        String[] rawLines = input.split("\\R");
        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i];

            StringBuilder cleanedLine = new StringBuilder();
            boolean inQuote = false;
            boolean escaped = false;

            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (escaped) {
                    escaped = false;
                    cleanedLine.append(c);
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    cleanedLine.append(c);
                    continue;
                }
                if (c == '"') {
                    inQuote = !inQuote;
                }
                if (c == '#' && !inQuote) {
                    break;
                }
                cleanedLine.append(c);
            }

            String trimmed = cleanedLine.toString().trim();
            if (!trimmed.isEmpty()) {
                result.add(new Line(i + 1, trimmed));
            }
        }
        return result;
    }

    /**
     * Finds the index of the first colon outside quotes in a line of assembly code
     * @param line the line of assembly code
     * @return the index of the colon, or -1 if not found
     */
    private static int findColonOutsideQuotes(String line) {
        boolean inQuote = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (c == ':' && !inQuote) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if a string is a valid label
     * @param str the string to check
     * @return true if it is a valid label, false otherwise
     */
    private static boolean isValidLabel(String str) {
        return str.matches("[a-zA-Z_.][a-zA-Z0-9_.]*");
    }

    /**
     * Checks if a line is a section directive (e.g., .text)
     * @param line the line to check
     * @return true if it is a section directive, false otherwise
     */
    private static boolean isSectionDirective(String line) {
        return line.equals(".text") || line.equals(".data") || line.equals(".bss") || line.equals(".rodata");
    }

    /**
     * Parses a section directive (e.g., .text) into a Section enum
     * @param line the line to parse
     * @return the Section enum corresponding to the directive
     */
    private static Section parseSectionDirective(String line) {
        if (line.startsWith(".section")) {
            String[] parts = line.split("\\s+");
            if (parts.length < 2) throw new IllegalArgumentException("Missing section name");
            line = parts[1];
        }
        return switch (line.trim()) {
            case ".text" -> Section.TEXT;
            case ".data" -> Section.DATA;
            case ".bss" -> Section.BSS;
            case ".rodata" -> Section.RODATA;
            default -> throw new IllegalArgumentException("Unknown section: " + line);
        };
    }

    /**
     * Gets the current address for a given section
     * @param section the section to get the address for
     * @param text the current text address
     * @param data the current data address
     * @param rodata the current rodata address
     * @param bss the current bss address
     * @return the current address for the given section
     */
    private static long getCurrentAddress(Section section, long text, long data, long rodata, long bss) {
        return switch (section) {
            case TEXT -> text;
            case DATA -> data;
            case RODATA -> rodata;
            case BSS -> bss;
        };
    }

    /**
     * Flushes pending labels to the label manager
     * @param pending the list of pending labels
     * @param lm the label manager to add labels to
     * @param address the current address of the label
     * @param size the size of the current label
     */
    private static void flushPendingLabels(List<String> pending, LabelManager lm, long address, int size) {
        for (String label : pending) {
            lm.addDataLabel(label, address, size);
        }
        pending.clear();
    }

    /**
     * Checks if the user assigned too much memory for a section
     * @param section the section to check
     * @param text the current text address
     * @param data the current data address
     * @param rodata the current rodata address
     * @param bss the current bss address
     */
    private static void validateOverflow(Section section, long text, long data, long rodata, long bss) {
        boolean overflow = switch (section) {
            case TEXT -> text >= MemoryLayout.CODE_LIMIT;
            case RODATA -> rodata >= MemoryLayout.READ_ONLY_DATA_LIMIT;
            case DATA -> data >= MemoryLayout.DATA_LIMIT;
            case BSS -> bss >= MemoryLayout.BSS_LIMIT;
        };
        if (overflow) {
            throw new IllegalArgumentException("Section " + section + " overflowed its memory limit.");
        }
    }

    /**
     * Computes the size of the arguments in a directive
     * @param line the line of assembly code containing the directive
     * @param currentAddr the current address of the instruction
     * @return the size of the directive in bytes
     */
    private static int computeDirectiveSize(String line, long currentAddr) {
        String[] parts = line.split("\\s+", 2);
        String directive = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        return switch (directive) {
            case ".byte" -> countArgs(args);
            case ".word" -> countArgs(args) * 2;
            case ".long" -> countArgs(args) * 4;
            case ".quad" -> countArgs(args) * 8;
            case ".ascii" -> computeStringSize(args, false);
            case ".asciz", ".string" -> computeStringSize(args, true);
            case ".space", ".skip", ".zero" -> {
                try {
                    yield Integer.decode(args.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid size for " + directive);
                }
            }
            case ".align" -> {
                try {
                    int alignment = Integer.decode(args.trim());
                    if (alignment <= 0) throw new IllegalArgumentException("Alignment must be positive");
                    long remainder = currentAddr % alignment;
                    if (remainder == 0) yield 0;
                    yield (int) (alignment - remainder);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid alignment value: " + args);
                }
            }
            default -> 0;
        };
    }

    /**
     * Counts the number of arguments in a directive
     * @param args the raw arguments string
     * @return the number of arguments
     */
    private static int countArgs(String args) {
        if (args.isBlank()) return 0;
        int count = 1;
        boolean inQuote = false;
        for (char c : args.toCharArray()) {
            if (c == '"') inQuote = !inQuote;
            if (c == ',' && !inQuote) count++;
        }
        return count;
    }

    /**
     * Computes the size of a string literal in bytes
     * @param args the raw arguments string, including quotes
     * @param nullTerminator whether to include a null terminator at the end (.ascii vs .asciz)
     * @return the size of the string literal in bytes
     */
    private static int computeStringSize(String args, boolean nullTerminator) {
        int totalBytes = 0;
        List<String> strings = splitDirectiveArgs(args);

        for (String s : strings) {
            s = s.trim();
            if (!s.startsWith("\"") || !s.endsWith("\"")) {
                throw new IllegalArgumentException("String literal must be quoted: " + s);
            }
            String content = s.substring(1, s.length() - 1);
            int length = processEscapesLength(content);
            totalBytes += length + (nullTerminator ? 1 : 0);
        }
        return totalBytes;
    }

    /**
     * Splits a directive argument string into a list of individual arguments
     * @param args The raw argument string to be split.
     * @return A list of arguments as strings
    */
    private static List<String> splitDirectiveArgs(String args) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean escaped = false;

        for (char c : args.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
            }
            if (c == ',' && !inQuote) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    /**
     * Computes the length of a string literal after escapes are processed
     * @param raw The raw string literal
     * @return The length of the string literal in bytes
     */
    private static int processEscapesLength(String raw) {
        int length = 0;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                length++;
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                length++;
            }
        }
        if (escaped) throw new IllegalArgumentException("Trailing escape character");
        return length;
    }

    /**
     * Writes a directive to memory based on the given assembly line
     *
     * @param line The line of assembly code containing the directive and its arguments.
     * @param currentAddr The current memory address where the directive data will be written.
     * @param memory The memory object where the directive will be written.
     * @return The new memory address after processing and writing the directive.
     */
    private static long writeDirective(String line, long currentAddr, Memory memory) {
        String[] parts = line.split("\\s+", 2);
        String directive = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (directive) {
            case ".byte" -> {
                for (String v : splitDirectiveArgs(args)) {
                    memory.writeByte(currentAddr, parseLong(v).byteValue());
                    currentAddr += 1;
                }
            }
            case ".word" -> {
                for (String v : splitDirectiveArgs(args)) {
                    memory.writeWord(currentAddr, parseLong(v).shortValue());
                    currentAddr += 2;
                }
            }
            case ".long" -> {
                for (String v : splitDirectiveArgs(args)) {
                    memory.writeLong(currentAddr, parseLong(v).intValue());
                    currentAddr += 4;
                }
            }
            case ".quad" -> {
                for (String v : splitDirectiveArgs(args)) {
                    memory.writeQuad(currentAddr, parseLong(v));
                    currentAddr += 8;
                }
            }
            case ".ascii" -> currentAddr = writeString(args, currentAddr, memory, false);
            case ".asciz", ".string" -> currentAddr = writeString(args, currentAddr, memory, true);
            case ".space", ".skip", ".zero" -> {
                int size = Integer.decode(args.trim());
                for (int i = 0; i < size; i++) memory.writeByte(currentAddr + i, (byte) 0);
                currentAddr += size;
            }
            case ".align" -> {
                int alignment = Integer.decode(args.trim());
                long remainder = currentAddr % alignment;
                if (remainder != 0) {
                    long padding = alignment - remainder;
                    for (int i = 0; i < padding; i++) memory.writeByte(currentAddr + i, (byte) 0);
                    currentAddr += padding;
                }
            }
        }
        return currentAddr;
    }

    /**
     * Parses the given string into a Long object.
     *
     * @param val the string to parse, in decimal or hex form
     * @return the parsed Long object
     */
    private static Long parseLong(String val) {
        val = val.trim();
        if (val.startsWith("-")) {
            return Long.decode(val);
        }
        if (val.toLowerCase().startsWith("0x")) {
            try {
                return Long.parseUnsignedLong(val.substring(2), 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex string: " + val);
            }
        }
        return Long.decode(val);
    }

    /**
     * Writes a string or a series of string literals to memory
     *
     * @param args The raw string argument(s) to be written
     * @param addr The starting memory address where the string data should be written
     * @param memory The memory object to which the data will be written
     * @param nullTerm A flag indicating whether each string should be null-terminated
     * @return The new memory address after writing the string data
     * @throws IllegalArgumentException If any string literal in the argument is not properly quoted
     */
    private static long writeString(String args, long addr, Memory memory, boolean nullTerm) {
        List<String> strings = splitDirectiveArgs(args);
        for (String s : strings) {
            s = s.trim();
            if (!s.startsWith("\"") || !s.endsWith("\"")) {
                throw new IllegalArgumentException("String literal must be quoted: " + s);
            }
            String content = s.substring(1, s.length() - 1);
            boolean escaped = false;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (escaped) {
                    byte b = switch (c) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        case '0' -> '\0';
                        case '\\' -> '\\';
                        case '"' -> '"';
                        default -> (byte) c;
                    };
                    memory.writeByte(addr++, b);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else {
                    memory.writeByte(addr++, (byte) c);
                }
            }
            if (nullTerm) {
                memory.writeByte(addr++, (byte) 0);
            }
        }
        return addr;
    }
}