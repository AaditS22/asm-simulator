package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.Memory;

import java.util.ArrayList;
import java.util.List;

// DISCLAIMER: This class was largely written by LLMs due to the complexity of string parsing
public class PrintfHandler {
    private static final int MAX_STRING_LENGTH = 4096;
    private static final int MAX_FORMAT_ARGS = 32;

    private static final String[] ARG_REGISTERS = {"rsi", "rdx", "rcx", "r8", "r9"};

    /**
     * Main method to execute the printf
     * @param state the state of the CPU when executing
     */
    public static void execute(CPUState state) {
        long formatAddr = state.getRegister("rdi", 8);
        if (formatAddr == 0) {
            throw new IllegalArgumentException("%RDI cannot be 0, printf was calling with a NULL format string");
        }

        String format = readNullTerminatedString(state.getMemory(), formatAddr);
        List<Long> args = collectArgs(state);

        String output = formatString(format, args, state.getMemory());
        state.getIOBuffer().append(output);
        state.setRegister("rax", 8, (long) output.length());
    }

    /**
     * Reads the raw bytes of a null-terminated string and converts them to a string
     * @param memory the memory to read from
     * @param address the address to read from
     * @return a string representation of the null-terminated string
     */
    private static String readNullTerminatedString(Memory memory, long address) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STRING_LENGTH; i++) {
            byte b = memory.readByte(address + i);
            if (b == 0) break;
            sb.append((char) (b & 0xFF));
            if (i == MAX_STRING_LENGTH - 1) {
                throw new IllegalArgumentException(
                        "String at address 0x" + Long.toHexString(address)
                                + " exceeds maximum length of " + MAX_STRING_LENGTH);
            }
        }
        return sb.toString();
    }

    /**
     * Collects the arguments for the printf into a list
     * @param state the state of the CPU when executing
     * @return a list of arguments
     */
    private static List<Long> collectArgs(CPUState state) {
        List<Long> args = new ArrayList<>();
        for (String reg : ARG_REGISTERS) {
            args.add(state.getRegister(reg, 8));
        }
        long rsp = state.getRegister("rsp", 8);
        for (int i = 0; i < MAX_FORMAT_ARGS - ARG_REGISTERS.length; i++) {
            args.add(state.getMemory().readQuad(rsp + (long) i * 8));
        }
        return args;
    }

    /**
     * Formats a string according to the specified format and arguments.
     * @param format the format string containing text and format specifiers
     * @param args a list of arguments to be formatted into the format string
     * @param memory the system memory
     * @return the formatted string after processing all format specifiers
     */
    private static String formatString(String format, List<Long> args, Memory memory) {
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;

        while (i < format.length()) {
            char c = format.charAt(i);

            if (c != '%') {
                result.append(processEscapeSequence(c, format, i));
                i++;
                continue;
            }

            i++;
            if (i >= format.length()) {
                throw new IllegalArgumentException("Incomplete format specifier at end of format string");
            }

            if (format.charAt(i) == '%') {
                result.append('%');
                i++;
                continue;
            }

            // Parse optional flags
            StringBuilder flags = new StringBuilder();
            while (i < format.length() && isFlag(format.charAt(i))) {
                flags.append(format.charAt(i));
                i++;
            }

            // Parse optional width
            StringBuilder width = new StringBuilder();
            while (i < format.length() && Character.isDigit(format.charAt(i))) {
                width.append(format.charAt(i));
                i++;
            }

            // Parse optional precision
            StringBuilder precision = new StringBuilder();
            if (i < format.length() && format.charAt(i) == '.') {
                i++;
                while (i < format.length() && Character.isDigit(format.charAt(i))) {
                    precision.append(format.charAt(i));
                    i++;
                }
            }

            // Parse optional length modifier
            String lengthMod = "";
            if (i < format.length() && (format.charAt(i) == 'l' || format.charAt(i) == 'h')) {
                char mod = format.charAt(i);
                i++;
                if (i < format.length() && format.charAt(i) == mod) {
                    lengthMod = String.valueOf(mod) + mod;
                    i++;
                } else {
                    lengthMod = String.valueOf(mod);
                }
            }

            if (i >= format.length()) {
                throw new IllegalArgumentException("Incomplete format specifier: missing conversion character");
            }

            char spec = format.charAt(i);
            i++;

            if (argIndex >= args.size()) {
                throw new IllegalArgumentException(
                        "Not enough arguments for format specifier '%" + spec + "' (argument " + (argIndex + 1) + ")");
            }

            long rawArg = args.get(argIndex++);
            String widthStr = width.toString();
            String precStr = precision.toString();
            String flagsStr = flags.toString();

            result.append(applySpecifier(spec, lengthMod, rawArg, widthStr, precStr, flagsStr, memory));
        }

        return result.toString();
    }

    /**
     * Applies a format specifier to a raw argument and produces a formatted string
     * according to the specifier and accompanying format parameters.
     *
     * @param spec      The format specifier indicating how the argument should be formatted.
     *                  Examples: 'd', 'x', 's', 'c'.
     * @param lengthMod The length modifier that determines the size of the argument type,
     *                  such as "l" or "h".
     * @param rawArg    The raw argument value that needs to be formatted.
     * @param width     The minimum width of the formatted output. Padded with spaces or zeros
     *                  if necessary.
     * @param precision The maximum number of characters or digits to use in formatting
     *                  (applicable to certain specifiers like 's' and 'f').
     * @param flags     Flags that modify the formatting behavior. Common flags include:
     *                  '+', '-', '0', ' ', and '#'.
     * @param memory    The memory to read from, used for specific specifiers like 's' to handle
     *                  memory-based strings.
     *
     * @return A formatted string based on the provided specifier and parameters.
     */
    private static String applySpecifier(char spec, String lengthMod, long rawArg,
                                         String width, String precision, String flags, Memory memory) {
        boolean leftAlign = flags.contains("-");
        boolean zeroPad = flags.contains("0") && !leftAlign;
        boolean showSign = flags.contains("+");
        boolean spaceSign = flags.contains(" ");

        int widthVal = width.isEmpty() ? 0 : Integer.parseInt(width);

        return switch (spec) {
            case 'd', 'i' -> {
                long signed = applySignedLength(rawArg, lengthMod);
                String sign = (signed < 0) ? "" : (showSign ? "+" : (spaceSign ? " " : ""));
                String num = sign + signed;
                yield pad(num, widthVal, zeroPad ? '0' : ' ', leftAlign);
            }
            case 'u' -> {
                long unsigned = applyUnsignedLength(rawArg, lengthMod);
                String num = Long.toUnsignedString(unsigned);
                yield pad(num, widthVal, zeroPad ? '0' : ' ', leftAlign);
            }
            case 'x' -> {
                long unsigned = applyUnsignedLength(rawArg, lengthMod);
                String prefix = flags.contains("#") ? "0x" : "";
                String num = prefix + Long.toUnsignedString(unsigned, 16);
                yield pad(num, widthVal, zeroPad ? '0' : ' ', leftAlign);
            }
            case 'X' -> {
                long unsigned = applyUnsignedLength(rawArg, lengthMod);
                String prefix = flags.contains("#") ? "0X" : "";
                String num = prefix + Long.toUnsignedString(unsigned, 16).toUpperCase();
                yield pad(num, widthVal, zeroPad ? '0' : ' ', leftAlign);
            }
            case 'o' -> {
                long unsigned = applyUnsignedLength(rawArg, lengthMod);
                String num = Long.toUnsignedString(unsigned, 8);
                yield pad(num, widthVal, zeroPad ? '0' : ' ', leftAlign);
            }
            case 'c' -> {
                char ch = (char) (rawArg & 0xFF);
                yield pad(String.valueOf(ch), widthVal, ' ', leftAlign);
            }
            case 's' -> {
                if (rawArg == 0) {
                    throw new IllegalArgumentException("printf %%s received NULL pointer");
                }
                String str = readNullTerminatedString(memory, rawArg);
                if (!precision.isEmpty()) {
                    int maxLen = Integer.parseInt(precision);
                    if (str.length() > maxLen) {
                        str = str.substring(0, maxLen);
                    }
                }
                yield pad(str, widthVal, ' ', leftAlign);
            }
            case 'p' -> {
                String addr = "0x" + Long.toUnsignedString(rawArg, 16);
                yield pad(addr, widthVal, ' ', leftAlign);
            }
            case 'f', 'e', 'E', 'g', 'G' ->
                    throw new UnsupportedOperationException(
                            "Floating-point format specifier '%" + spec + "' is not supported "
                                    + "(the simulator does not simulate floating-point registers)");
            case 'n' ->
                    throw new UnsupportedOperationException(
                            "The %%n specifier is not supported for security reasons");
            default ->
                    throw new IllegalArgumentException("Unknown format specifier: '%" + spec + "'");
        };
    }

    /**
     * Applies the length modifier to the raw signed argument value.
     * @param raw the raw argument value
     * @param mod the length modifier
     * @return the modified argument value
     */
    private static long applySignedLength(long raw, String mod) {
        return switch (mod) {
            case "hh" -> (byte) raw;
            case "h" -> (short) raw;
            case "l", "ll", "" -> raw;
            default -> raw;
        };
    }

    /**
     * Applies the length modifier to the raw unsigned argument value.
     * @param raw the raw argument value
     * @param mod the length modifier
     * @return the modified argument value.
     */
    private static long applyUnsignedLength(long raw, String mod) {
        return switch (mod) {
            case "hh" -> raw & 0xFFL;
            case "h" -> raw & 0xFFFFL;
            case "l", "ll", "" -> raw;
            default -> raw;
        };
    }

    /**
     * Pads a string to the specified width with the specified padding character.
     * @param s the string to pad
     * @param width the desired width of the padded string
     * @param padChar the character to use for padding
     * @param leftAlign whether to pad on the left or right side
     * @return the padded string
     */
    private static String pad(String s, int width, char padChar, boolean leftAlign) {
        if (s.length() >= width) return s;
        int needed = width - s.length();
        String padding = String.valueOf(padChar).repeat(needed);
        return leftAlign ? s + padding : padding + s;
    }

    /**
     * Checks if a character is a valid flag character for printf format specifiers.
     * @param c the character to check
     * @return true if it is a valid flag character, false otherwise
     */
    private static boolean isFlag(char c) {
        return c == '-' || c == '+' || c == ' ' || c == '0' || c == '#';
    }

    /**
     * Processes an escape sequence in the format string.
     * @param c the current character
     * @param format the format string
     * @param i the index of the current character in the format string
     * @return the processed character
     */
    private static char processEscapeSequence(char c, String format, int i) {
        return c;
    }
}