package io.github.AaditS22.asmsimulator.web.dto;

public record StepResponseDto(
        String description,
        String mnemonic,
        String output,
        boolean hasOutput,
        StateDto state,
        String error
) {}