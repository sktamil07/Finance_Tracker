package com.financetracker.investment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "UpcomingInvestmentResponse", description = "A SIP instalment or a maturity landing in the current or next month")
public record UpcomingInvestmentResponse(
        Long investmentId,
        String name,
        String categoryName,
        BigDecimal amount,
        Kind kind,
        @Schema(description = "When it lands: the SIP date or the maturity date") LocalDate dueDate,
        String roiNotes
) {

    public enum Kind {
        SIP,
        MATURITY
    }
}
