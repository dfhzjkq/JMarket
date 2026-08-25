package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {
}
