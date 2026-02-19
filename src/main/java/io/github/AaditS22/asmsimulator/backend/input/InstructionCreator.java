package io.github.AaditS22.asmsimulator.backend.input;

import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.AddInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.DecInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.DivInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.IDivInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.IMulInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.IncInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.MulInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.arithmetic.SubInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.branching.*;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.AndInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.CmpInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.NegInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.NotInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.OrInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.ShlInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.ShrInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.TestInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.logical.XorInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.LeaInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovzbInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.MovzwInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.PopInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.movement.PushInstruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.MemoryOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.RegisterOperand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;
import java.util.Set;

public class InstructionCreator {

    private static final Set<String> SIZED_MNEMONICS = Set.of(
            "mov", "add", "sub", "inc", "dec", "mul", "imul", "div", "idiv",
            "and", "or", "xor", "not", "neg", "test", "cmp",
            "shl", "shr", "sal",
            "lea", "push", "pop"
    );

    private static final Set<String> UNSIZED_MNEMONICS = Set.of(
            "ret", "jmp", "call", "loop",
            "je", "jne", "jg", "jge", "jl", "jle"
    );

    private static final Set<String> BRANCH_MNEMONICS = Set.of(
            "jmp", "call", "loop",
            "je", "jne", "jg", "jge", "jl", "jle"
    );

    public InstructionCreator() {
    }

    /**
     * Checks if a mnemonic is a branch mnemonic
     * @param mnemonic the mnemonic to check
     * @return true if it is a branch mnemonic, false otherwise
     */
    public static boolean isBranchMnemonic(String mnemonic) {
        return BRANCH_MNEMONICS.contains(mnemonic.trim().toLowerCase());
    }

    /**
     * Creates a new instruction based on its mnemonic and operands
     * @param mnemonic the mnemonic of the instruction (e.g., MOV)
     * @param operands the list of its operands
     * @return a new instruction with the correct type representing the input one
     */
    public static Instruction create(String mnemonic, List<Operand> operands) {
        String lower = mnemonic.trim().toLowerCase();

        if (lower.startsWith("movzb")) {
            validateMemoryOperands(lower, operands);
            return createMovzb(lower, operands);
        }
        if (lower.startsWith("movzw")) {
            validateMemoryOperands(lower, operands);
            return createMovzw(lower, operands);
        }

        if (UNSIZED_MNEMONICS.contains(lower)) {
            return createUnsized(lower, operands);
        }

        Size size = null;
        String base;

        if (!lower.isEmpty()) {
            char lastChar = lower.charAt(lower.length() - 1);
            Size suffixSize = Size.getSize(lastChar);
            String candidate = lower.substring(0, lower.length() - 1);

            if (suffixSize != null && SIZED_MNEMONICS.contains(candidate)) {
                size = suffixSize;
                base = candidate;
            } else if (SIZED_MNEMONICS.contains(lower)) {
                base = lower;
                size = inferSize(operands);
            } else {
                throw new IllegalArgumentException(
                        "Unknown instruction: " + mnemonic);
            }
        } else {
            throw new IllegalArgumentException("Empty mnemonic");
        }

        if (size == null) {
            throw new IllegalArgumentException(
                    "Cannot determine operand size for '" + mnemonic
                            + "'. Use a size suffix (b/w/l/q) or provide a register operand so the" +
                            " system can automatically infer the size.");
        }

        validateMemoryOperands(base, operands);

        return createSized(base, lower, size, operands);
    }

    /**
     * Helper method to create a sized instruction
     * @param base the base mnemonic without the size suffix (e.g., MOV)
     * @param fullMnemonic the entire mnemonic with size suffix (e.g., MOVQ)
     * @param size the size of the instruction as a Size enum
     * @param operands the list of the operands in the instruction
     * @return a new instruction object corresponding to the correct mnemonic
     */
    private static Instruction createSized(String base, String fullMnemonic,
                                           Size size, List<Operand> operands) {
        return switch (base) {
            case "mov" -> new MovInstruction(fullMnemonic, size, operands);
            case "add" -> new AddInstruction(fullMnemonic, size, operands);
            case "sub" -> new SubInstruction(fullMnemonic, size, operands);
            case "inc" -> new IncInstruction(fullMnemonic, size, operands);
            case "dec" -> new DecInstruction(fullMnemonic, size, operands);
            case "mul" -> new MulInstruction(fullMnemonic, size, operands);
            case "imul" -> new IMulInstruction(fullMnemonic, size, operands);
            case "div" -> new DivInstruction(fullMnemonic, size, operands);
            case "idiv" -> new IDivInstruction(fullMnemonic, size, operands);
            case "and" -> new AndInstruction(fullMnemonic, size, operands);
            case "or" -> new OrInstruction(fullMnemonic, size, operands);
            case "xor" -> new XorInstruction(fullMnemonic, size, operands);
            case "not" -> new NotInstruction(fullMnemonic, size, operands);
            case "neg" -> new NegInstruction(fullMnemonic, size, operands);
            case "test" -> new TestInstruction(fullMnemonic, size, operands);
            case "cmp" -> new CmpInstruction(fullMnemonic, size, operands);
            case "shl", "sal" -> new ShlInstruction(fullMnemonic, size, operands);
            case "shr" -> new ShrInstruction(fullMnemonic, size, operands);
            case "lea" -> new LeaInstruction(fullMnemonic, size, operands);
            case "push" -> new PushInstruction(fullMnemonic, size, operands);
            case "pop" -> new PopInstruction(fullMnemonic, size, operands);
            default -> throw new IllegalArgumentException(
                    "The instruction: " + fullMnemonic + " is either invalid or not yet supported");
        };
    }

    /**
     * Creates a new unsized instruction
     * @param mnemonic the mnemonic of the instruction (e.g., RET)
     * @param operands the list of operands in the instruction
     * @return a new instruction object corresponding to the correct mnemonic
     */
    private static Instruction createUnsized(String mnemonic,
                                             List<Operand> operands) {
        return switch (mnemonic) {
            case "ret" -> new RetInstruction(mnemonic, Size.QUAD, operands);
            case "jmp" -> new JmpInstruction(mnemonic, Size.QUAD, operands);
            case "call" -> new CallInstruction(mnemonic, Size.QUAD, operands);
            case "loop" -> new LoopInstruction(mnemonic, Size.QUAD, operands);
            default -> new ConditionalJumpInstruction(
                    mnemonic, Size.QUAD, operands);
        };
    }

    /**
     * Creates a movzb instruction
     * @param mnemonic the mnemonic of the instruction including size suffix (e.g., MOVZB)
     * @param operands the list of operands in the instruction
     * @return a new movzb instruction
     */
    private static Instruction createMovzb(String mnemonic,
                                           List<Operand> operands) {
        char destSuffix = mnemonic.charAt(mnemonic.length() - 1);
        Size destSize = Size.getSize(destSuffix);
        if (destSize == null || destSize == Size.BYTE) {
            throw new IllegalArgumentException(
                    "Invalid movzb destination size suffix in: " + mnemonic);
        }
        return new MovzbInstruction(mnemonic, Size.BYTE, operands);
    }

    /**
     * Creates a movzw instruction
     * @param mnemonic the mnemonic of the instruction including size suffix (e.g., MOVZWQ)
     * @param operands the list of operands in the instruction
     * @return a new movzw instruction
     */
    private static Instruction createMovzw(String mnemonic,
                                           List<Operand> operands) {
        char destSuffix = mnemonic.charAt(mnemonic.length() - 1);
        Size destSize = Size.getSize(destSuffix);
        if (destSize == null || destSize == Size.BYTE || destSize == Size.WORD) {
            throw new IllegalArgumentException(
                    "Invalid movzw destination size suffix in: " + mnemonic);
        }
        return new MovzwInstruction(mnemonic, Size.WORD, operands);
    }

    /**
     * Helper method to infer the size of an instruction based on its operands
     * @param operands the list of operands in the instruction
     * @return an inferred size as a Size enum, null if it could not be inferred
     */
    private static Size inferSize(List<Operand> operands) {
        Size inferred = null;
        for (Operand op : operands) {
            if (op instanceof RegisterOperand regOp) {
                Size regSize = regOp.getSize();
                if (inferred != null && inferred != regSize) {
                    throw new IllegalArgumentException(
                            "Register operands have mismatched sizes");
                }
                inferred = regSize;
            }
        }
        return inferred;
    }

    /**
     * Validates that the instruction does not have two or more memory operands
     * @param base the base mnemonic of the instruction to adapt for the special case of lea
     * @param operands the list of operands in the instruction to validate
     * @throws IllegalArgumentException if the instruction has two or more memory operands
     */
    private static void validateMemoryOperands(String base, List<Operand> operands) {
        if (base.equals("lea")) {
            return;
        }

        int memCount = 0;
        for (Operand op : operands) {
            if (op instanceof MemoryOperand) {
                memCount++;
            }
        }
        if (memCount > 1) {
            throw new IllegalArgumentException(
                    base.toUpperCase() + " cannot have two or more memory operands");
        }
    }
}