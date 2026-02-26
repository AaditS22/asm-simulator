package io.github.AaditS22.asmsimulator.web.dto;

import java.util.List;
import java.util.Map;

public record StateDto(
        String status,
        int stepCount,
        int currentInstructionIndex,
        int currentLineNumber,
        List<Integer> instructionLineMap,
        Map<String, Long> registers,
        FlagsDto flags,
        List<StackRowDto> stack,
        List<MemoryEntryDto> memory
) {
    public record FlagsDto(boolean zf, boolean cf, boolean sf, boolean of) {}

    public record StackRowDto(
            String address,
            long addressRaw,
            long value,
            String valueHex,
            boolean isRsp,
            boolean isRbp
    ) {}

    public record MemoryEntryDto(
            String address,
            long addressRaw,
            String label,
            String reason,
            long value,
            String valueHex,
            int size,
            boolean changed
    ) {}
}