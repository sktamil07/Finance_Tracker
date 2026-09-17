package com.financetracker.investment;

import com.financetracker.common.util.MonthUtils;
import com.financetracker.investment.dto.InvestmentRequest;
import com.financetracker.investment.dto.InvestmentResponse;
import com.financetracker.investment.dto.UpcomingInvestmentResponse;
import com.financetracker.investment.service.InvestmentService;
import com.financetracker.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Investments")
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @Operation(summary = "List investments, optionally filtered to one month")
    @GetMapping
    public List<InvestmentResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM; omit for every month", example = "2026-04")
            @RequestParam(required = false) String month) {
        return investmentService.list(principal.id(), MonthUtils.parseOptionalYearMonth(month));
    }

    @Operation(summary = "SIP instalments and maturities due between today and the end of next month")
    @GetMapping("/upcoming")
    public List<UpcomingInvestmentResponse> upcoming(@AuthenticationPrincipal AuthPrincipal principal) {
        return investmentService.upcoming(principal.id());
    }

    @Operation(summary = "Record an investment")
    @ApiResponse(responseCode = "400", description = "categoryId is not an INVESTMENT category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                     @Valid @RequestBody InvestmentRequest request) {
        return investmentService.create(principal.id(), request);
    }

    @Operation(summary = "Update an investment")
    @ApiResponse(responseCode = "403", description = "The investment belongs to another user")
    @PutMapping("/{id}")
    public InvestmentResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody InvestmentRequest request) {
        return investmentService.update(principal.id(), id, request);
    }

    @Operation(summary = "Delete an investment")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        investmentService.delete(principal.id(), id);
    }
}
