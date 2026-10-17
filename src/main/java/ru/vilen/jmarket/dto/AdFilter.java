package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import ru.vilen.jmarket.entity.AdStatus;

import java.math.BigDecimal;

public record AdFilter(
    String query,
    @DecimalMin("0.00") BigDecimal minPrice,
    @DecimalMin("0.00") BigDecimal maxPrice,
    @Positive Long authorId,
    AdStatus status,
    @Positive Long categoryId
) {
}
