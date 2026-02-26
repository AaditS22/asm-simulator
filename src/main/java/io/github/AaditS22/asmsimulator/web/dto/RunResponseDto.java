package io.github.AaditS22.asmsimulator.web.dto;

public record RunResponseDto(
        String output,
        String lastDescription,
        String lastMnemonic,
        long exitCode,
        StateDto state,
        String error
) {}