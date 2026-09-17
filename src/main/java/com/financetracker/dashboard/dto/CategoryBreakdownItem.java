package com.financetracker.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "CategoryBreakdownItem")
public record CategoryBreakdownItem(
        Long categoryId,
        String categoryName,
        String categoryIcon,
        BigDecimal total,
        @Schema(description = "Share of this bucket's total, 2dp") BigDecimal percentage
) {
}
