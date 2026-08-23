package ru.vilen.jmarket.dto;

public record ErrorResponse(
        int status,
        String message
) {
}
