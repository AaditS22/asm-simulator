package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.instructions.operands.ImmediateOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;

import java.util.ArrayList;
import java.util.List;

public class OperandParser {
    public OperandParser() {}

    /**
     * Parses a raw operand string into an Operand object of the correct type
     *
     * @param raw the raw string
     * @param isBranchTarget a boolean for if the operand is the target of a branch instruction
     * @return the Operand instance corresponding to the parsed raw input
     */
    public static Operand parse(String raw, boolean isBranchTarget) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty operand");
        }

        if (trimmed.startsWith("$")) {
            return new ImmediateOperand(trimmed);
        }

        if (trimmed.startsWith("%")) {
            return new RegisterOperand(trimmed);
        }

        if (trimmed.contains("(")) {
            return new MemoryOperand(trimmed);
        }

        if (isNumber(trimmed)) {
            return new MemoryOperand(trimmed);
        }

        if (isBranchTarget) {
            return new LabelOperand(trimmed);
        }

        return new MemoryOperand(trimmed);
    }

    /**
     * Parses all the operands in an operand string
     * @param operandString the string of raw operands
     * @param isBranchTarget whether the operands are the target(s) of a branch instruction
     * @return a list containing operands of the correct type for each one in the string
     */
    public static List<Operand> parseAll(String operandString,
                                         boolean isBranchTarget) {
        List<Operand> operands = new ArrayList<>();
        if (operandString == null || operandString.trim().isEmpty()) {
            return operands;
        }

        List<String> tokens = splitOperands(operandString.trim());
        for (String token : tokens) {
            operands.add(parse(token, isBranchTarget));
        }
        return operands;
    }

    /**
     * Splits a string of operands with correct handling of commas nested inside memory operands
     * @param operandString the string containing all operands
     * @return a list containing strings, where each string represents one operand
     */
    static List<String> splitOperands(String operandString) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < operandString.length(); i++) {
            char c = operandString.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                String token = operandString.substring(start, i).trim();
                if (token.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Empty operand found in: " + operandString);
                }
                result.add(token);
                start = i + 1;
            }
        }

        String last = operandString.substring(start).trim();
        if (last.isEmpty() && !result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Trailing comma in operand list: " + operandString);
        }
        if (!last.isEmpty()) {
            result.add(last);
        }

        return result;
    }

    /**
     * Checks if a raw operand is a label operand
     * @param raw the raw operand string
     * @return true for if it is a label operand, else false
     */
    public static boolean isLabelOperand(String raw) {
        String trimmed = raw.trim();
        return !trimmed.startsWith("$")
                && !trimmed.startsWith("%")
                && !trimmed.contains("(")
                && !isNumber(trimmed);
    }

    /**
     * Checks if a string is a number (immediate)
     * @param s the string to check
     * @return true if it is a number (in hex or decimal form), false otherwise
     */
    private static boolean isNumber(String s) {
        if (s.isEmpty()) {
            return false;
        }
        String check = s;
        if (check.startsWith("-") || check.startsWith("+")) {
            check = check.substring(1);
        }
        if (check.isEmpty()) {
            return false;
        }
        if (check.startsWith("0x") || check.startsWith("0X")) {
            return check.length() > 2 && check.substring(2).chars()
                    .allMatch(c -> "0123456789abcdefABCDEF".indexOf(c) >= 0);
        }
        return check.chars().allMatch(Character::isDigit);
    }
}