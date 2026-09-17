package com.financetracker.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "TopExpenseCategory")
public record TopExpenseCategory(
        Long categoryId,
        String categoryName,
        String categoryIcon,
        @Schema(description = "Total spend in this category across the whole window") BigDecimal total,
        @Schema(description = "Share of total expense across the window, 2dp") BigDecimal percentageOfWindow
) {
}
