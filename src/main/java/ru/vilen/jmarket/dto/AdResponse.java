package ru.vilen.jmarket.dto;

import ru.vilen.jmarket.entity.AdStatus;

import java.math.BigDecimal;

public record AdResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Long authorId,
        AdStatus status
) {
}
