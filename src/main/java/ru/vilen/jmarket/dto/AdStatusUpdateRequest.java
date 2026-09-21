package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.NotNull;
import ru.vilen.jmarket.entity.AdStatus;

public record AdStatusUpdateRequest(
        @NotNull AdStatus status
) {
}
