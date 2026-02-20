package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;

import java.util.ArrayList;
import java.util.List;

// DISCLAIMER: This class was largely written with the help of LLMs due to the complexity of format string parsing
public class ScanfHandler {
    private static final int MAX_STRING_LENGTH = 4096;
    private static final int MAX_FORMAT_ARGS = 32;
    private static final String[] ARG_REGISTERS = {"rsi", "rdx", "rcx", "r8", "r9"};

    /**
     * The main method to execute a scanf call
     * Runs twice, with the first time setting "WAITING FOR INPUT" to true, and the second time collecting it
     * @param state the state of the CPU when executing
     */
    public static void execute(CPUState state) {
        if (!state.getIOBuffer().hasInput()) {
            state.getIOBuffer().setWaitingForInput(true);
            return;
        }

        long formatAddr = state.getRegister("rdi", 8);
        if (formatAddr == 0) {
            throw new IllegalArgumentException("%RDI cannot be 0, scanf was called with a NULL format string");
        }

        String format = readNullTerminatedString(state.getMemory(), formatAddr);
        String input = state.getIOBuffer().consumeInput();
        List<Long> pointers = collectPointers(state);

        int matched = parseInput(format, input, pointers, state.getMemory());
        state.setRegister("rax", 8, (long) matched);
    }

    /**
     * Reads a null terminated string from memory
     * @param memory the memory to read from
     * @param address the address to read from
     * @return the string read
     */
    public static String readNullTerminatedString(Memory memory, long address) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STRING_LENGTH; i++) {
            byte b = memory.readByte(address + i);
            if (b == 0) break;
            sb.append((char) (b & 0xFF));
            if (i == MAX_STRING_LENGTH - 1) {
                throw new IllegalArgumentException(
                        "The string at address 0x" + Long.toHexString(address) + " exceeds "
                                + MAX_STRING_LENGTH + " characters without a null terminator. Make sure "
                                + "your string is defined with '.asciz' (which adds '\\0' automatically) "
                                + "rather than '.ascii', and that the address in %rdi is correct.");
            }
        }
        return sb.toString();
    }

    /**
     * Collects the pointers for where to store collected input
     * @param state the state of the CPU when executing
     * @return a list of pointers
     */
    private static List<Long> collectPointers(CPUState state) {
        List<Long> pointers = new ArrayList<>();
        for (String reg : ARG_REGISTERS) {
            pointers.add(state.getRegister(reg, 8));
        }
        long rsp = state.getRegister("rsp", 8);
        for (int i = 0; i < MAX_FORMAT_ARGS - ARG_REGISTERS.length; i++) {
            pointers.add(state.getMemory().readQuad(rsp + (long) i * 8));
        }
        return pointers;
    }

    /**
     * Parses the input string according to the format string
     * @param format the format string
     * @param input the input string
     * @param pointers the list of pointers to store parsed input to
     * @param memory the memory of the cpu
     * @return an integer representing the number of arguments matched
     */
    private static int parseInput(String format, String input, List<Long> pointers, Memory memory) {
        int argIndex = 0;
        int inputPos = 0;
        int matched = 0;
        int i = 0;

        while (i < format.length()) {
            char c = format.charAt(i);

            if (Character.isWhitespace(c)) {
                inputPos = skipWhitespace(input, inputPos);
                i++;
                continue;
            }

            if (c != '%') {
                if (inputPos < input.length() && input.charAt(inputPos) == c) {
                    inputPos++;
                }
                i++;
                continue;
            }

            i++;
            if (i >= format.length()) break;

            if (format.charAt(i) == '%') {
                if (inputPos < input.length() && input.charAt(inputPos) == '%') inputPos++;
                i++;
                continue;
            }

            // Parse optional width
            StringBuilder widthSb = new StringBuilder();
            while (i < format.length() && Character.isDigit(format.charAt(i))) {
                widthSb.append(format.charAt(i++));
            }
            int width = widthSb.length() > 0 ? Integer.parseInt(widthSb.toString()) : -1;

            // Parse optional length modifier
            String lengthMod = "";
            if (i < format.length()) {
                char lc = format.charAt(i);
                if (lc == 'h') {
                    i++;
                    if (i < format.length() && format.charAt(i) == 'h') { lengthMod = "hh"; i++; }
                    else lengthMod = "h";
                } else if (lc == 'l') {
                    i++;
                    if (i < format.length() && format.charAt(i) == 'l') { lengthMod = "ll"; i++; }
                    else lengthMod = "l";
                }
            }

            if (i >= format.length()) break;
            char spec = format.charAt(i++);

            if (argIndex >= pointers.size()) break;
            long addr = pointers.get(argIndex++);

            switch (spec) {
                case 'd', 'i' -> {
                    inputPos = skipWhitespace(input, inputPos);
                    String token = readNonWhitespace(input, inputPos, width);
                    if (token.isEmpty()) throw new IllegalArgumentException(
                            "scanf ran out of input while trying to read a signed integer for '%" + lengthMod + spec +
                                    "'. The format string expects more values than were provided. "
                                    + "Make sure your input contains enough space-separated tokens" +
                                    " to match every specifier.");
                    inputPos += token.length();
                    long val;
                    try { val = Long.parseLong(token); }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "scanf expected a signed integer for '%" + lengthMod + spec + "' but received '"
                                        + token + "'. Make sure the input token is a whole number with no letters or "
                                        + "special characters (a leading '-' for negatives is allowed).");
                    }
                    writeInteger(memory, addr, val, lengthMod);
                    matched++;
                }
                case 'u' -> {
                    inputPos = skipWhitespace(input, inputPos);
                    String token = readNonWhitespace(input, inputPos, width);
                    if (token.isEmpty()) throw new IllegalArgumentException(
                            "scanf ran out of input while trying to read an unsigned integer for '%" + lengthMod +
                                    "u'. The format string expects more values than were provided. "
                                    + "Make sure your input contains enough space-separated " +
                                    "tokens to match every specifier.");
                    inputPos += token.length();
                    long val;
                    try { val = Long.parseUnsignedLong(token); }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "scanf expected an unsigned integer for '%" + lengthMod + "u' but received '"
                                        + token + "'. Unsigned integers must be non-negative whole numbers "
                                        + "with no letters or special characters.");
                    }
                    writeInteger(memory, addr, val, lengthMod);
                    matched++;
                }
                case 's' -> {
                    inputPos = skipWhitespace(input, inputPos);
                    String token = readNonWhitespace(input, inputPos, width);
                    if (token.isEmpty()) throw new IllegalArgumentException(
                            "scanf ran out of input while trying to read a string for '%s'. "
                                    + "The format string expects more values than were provided. "
                                    + "Make sure your input contains enough space-separated" +
                                    " tokens to match every specifier.");
                    inputPos += token.length();
                    writeNullTerminatedString(memory, addr, token);
                    matched++;
                }
                case 'c' -> {
                    if (inputPos >= input.length()) throw new IllegalArgumentException(
                            "scanf ran out of input while trying to read a character for '%c'. "
                                    + "Unlike other specifiers, '%c' does not skip whitespace — it reads "
                                    + "the very next character, including spaces and newlines. "
                                    + "Make sure there is at least one character" +
                                    " remaining in the input at this point.");
                    memory.writeByte(addr, (byte) input.charAt(inputPos++));
                    matched++;
                }
                default -> throw new UnsupportedOperationException(
                        "scanf format specifier '%" + spec + "' is not supported by this simulator. "
                                + "Supported specifiers are: %d and %i (signed integer), %u (unsigned integer), "
                                + "%s (string), and %c (single character), along with length modifiers "
                                + "hh, h, l, and ll.");
            }
        }

        return matched;
    }

    /**
     * Skips over consecutive whitespace characters in a string from a position
     * @param s the string to be processed
     * @param pos the starting position in the string to begin skipping whitespace
     * @return the position of the first non-whitespace character in the string after the position
     */
    private static int skipWhitespace(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        return pos;
    }

    /**
     * Reads a sequence of non-whitespace characters from a string starting at a given position
     * @param input the string to read from
     * @param pos the starting position in the string
     * @param maxWidth the maximum number of characters to read
     * @return a string containing the sequence of non-whitespace characters read from the input
     */
    private static String readNonWhitespace(String input, int pos, int maxWidth) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (pos < input.length()) {
            if (Character.isWhitespace(input.charAt(pos))) break;
            if (maxWidth > 0 && count >= maxWidth) break;
            sb.append(input.charAt(pos++));
            count++;
        }
        return sb.toString();
    }

    /**
     * Writes an integer to memory, according to the length modifier
     * @param memory the memory to write to
     * @param addr the address to write to
     * @param val the value to write
     * @param mod the length modifier
     */
    private static void writeInteger(Memory memory, long addr, long val, String mod) {
        switch (mod) {
            case "hh" -> memory.writeByte(addr, (byte) val);
            case "h"  -> memory.writeWord(addr, (short) val);
            case "l", "ll" -> memory.writeQuad(addr, val);
            default        -> memory.writeLong(addr, (int) val);
        }
    }

    /**
     * Writes a null-terminated string to memory, with the null character at the end.
     * @param memory the memory to write to
     * @param addr the address to write to
     * @param s the string to write
     */
    private static void writeNullTerminatedString(Memory memory, long addr, String s) {
        for (int i = 0; i < s.length(); i++) {
            memory.writeByte(addr + i, (byte) s.charAt(i));
        }
        memory.writeByte(addr + s.length(), (byte) 0);
    }
}