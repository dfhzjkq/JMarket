package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AdFilter(
    String query,
    @DecimalMin("0.00") BigDecimal minPrice,
    @DecimalMin("0.00") BigDecimal maxPrice,
    Long authorId
) {
}
