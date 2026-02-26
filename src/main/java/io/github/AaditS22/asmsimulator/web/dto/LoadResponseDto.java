package io.github.AaditS22.asmsimulator.web.dto;

public record LoadResponseDto(
        boolean success,
        String error,
        StateDto state
) {}