package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Positive Long authorId
) {
}
