package com.example.product.serviceb.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
