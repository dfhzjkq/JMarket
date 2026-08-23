package ru.vilen.jmarket.dto;

import java.math.BigDecimal;

public record AdResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Long authorId
) {
}
