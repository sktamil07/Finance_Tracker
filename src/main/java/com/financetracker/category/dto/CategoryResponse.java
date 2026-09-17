package com.financetracker.category.dto;

import com.financetracker.category.domain.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CategoryResponse")
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        String icon,
        @Schema(description = "True for categories seeded at registration") boolean systemDefault,
        Instant createdAt
) {
}
