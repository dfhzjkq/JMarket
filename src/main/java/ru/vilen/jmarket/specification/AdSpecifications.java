package ru.vilen.jmarket.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.vilen.jmarket.entity.Ad;

import java.math.BigDecimal;

public final class AdSpecifications {

    private AdSpecifications() {
    }

    public static Specification<Ad> hasMinPrice(BigDecimal minPrice) {
        if (minPrice == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        minPrice
                );
    }

    public static Specification<Ad> hasMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        maxPrice
                );
    }
}
