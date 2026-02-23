package io.github.AaditS22.asmsimulator.backend.instructions.branching;

import io.github.AaditS22.asmsimulator.backend.cpu.CPUState;
import io.github.AaditS22.asmsimulator.backend.cpu.Flags;
import io.github.AaditS22.asmsimulator.backend.cpu.LabelManager;
import io.github.AaditS22.asmsimulator.backend.instructions.Instruction;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.LabelOperand;
import io.github.AaditS22.asmsimulator.backend.instructions.operands.Operand;
import io.github.AaditS22.asmsimulator.backend.util.Size;

import java.util.List;

public class ConditionalJumpInstruction extends Instruction {
    private static final List<String> SUPPORTED_CONDITIONS = List.of("e", "ne", "g", "ge", "l", "le");
    private final String condition;

    public ConditionalJumpInstruction(String mnemonic, Size size, List<Operand> operands) {
        super(mnemonic, size, operands);
        validateOperandCount(1);

        if (mnemonic.trim().length() >= 2) {
            condition = mnemonic.trim().toLowerCase().substring(1);
            if (!SUPPORTED_CONDITIONS.contains(condition)) {
                throw new IllegalArgumentException("The jump condition: " + condition + " is either " +
                        "not supported yet or is invalid");
            }
        } else {
            throw new IllegalArgumentException("Invalid mnemonic for conditional jump instruction");
        }

        if (!(operands.get(0) instanceof LabelOperand)) {
            throw new IllegalArgumentException(mnemonic.toUpperCase() + " requires a label operand as target");
        }
    }

    /**
     * Helper method to check if the instruction should jump
     * @param state the current CPU state
     * @return a boolean for whether the instruction should jump or not
     */
    private boolean shouldJump(CPUState state) {
        Flags flags = state.getFlags();
        return switch (condition) {
            case "e" -> flags.isZero();
            case "ne" -> !flags.isZero();
            case "g"  -> !flags.isZero() && (flags.isNegative() == flags.isOverflow());
            case "ge" -> flags.isNegative() == flags.isOverflow();
            case "l"  -> flags.isNegative() != flags.isOverflow();
            case "le" -> flags.isZero() || (flags.isNegative() != flags.isOverflow());
            default -> throw new IllegalArgumentException("The jump condition: " + condition + " is either " +
                    "not supported yet or is invalid");
        };
    }

    @Override
    public void execute(CPUState state, LabelManager labelManager) {
        if (shouldJump(state)) {
            Operand target = operands.get(0);
            long address = target.getValue(state, labelManager, Size.QUAD);
            state.setPC(address);
        } else {
            state.nextInstruction();
        }
    }

    @Override
    public String getDescription(CPUState state, LabelManager labelManager) {
        String conditionDescription = switch (condition) {
            case "e"  -> "the last comparison was equal";
            case "ne" -> "the last comparison was not equal";
            case "g"  -> "the last comparison was greater";
            case "ge" -> "the last comparison was greater or equal";
            case "l"  -> "the last comparison was less";
            case "le" -> "the last comparison was less or equal";
            default   -> "the condition is invalid";
        };
        return "Jumped to the instruction at " + operands.get(0).getDescription(state, labelManager)
                + " based on if " + conditionDescription + " (by examining the CPU flags set by the comparison).";
    }
}
