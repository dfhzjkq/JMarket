package ru.vilen.jmarket.dto;

import java.util.List;

public record AdPageResponse(
        List<AdResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
