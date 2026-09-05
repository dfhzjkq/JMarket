package ru.vilen.jmarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @Email @NotBlank @Size(max = 255) String email
) {
}
