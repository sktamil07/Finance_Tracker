package com.financetracker.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "BondAlertResponse", description = "An investment maturing in the requested month or the one after it")
public record BondAlertResponse(
        Long investmentId,
        String name,
        String categoryName,
        BigDecimal amount,
        LocalDate maturityDate,
        @Schema(description = "True when the maturity lands in the requested month itself") boolean maturingThisMonth,
        String roiNotes
) {
}
