package ru.vilen.jmarket.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.vilen.jmarket.entity.Ad;

import java.math.BigDecimal;
import java.util.Locale;

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

    public static Specification<Ad> hasAuthorId(Long authorId) {
        if (authorId == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Ad> hasQuery(String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                pattern
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                pattern
                        )
                );
    }
}
