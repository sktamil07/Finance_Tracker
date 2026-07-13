package com.bharath.financetracker.income;

import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.income.dto.IncomeRequest;
import com.bharath.financetracker.income.dto.IncomeResponse;
import com.bharath.financetracker.income.service.IncomeService;
import com.bharath.financetracker.security.AuthPrincipal;
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

@Tag(name = "Income", description = "Multiple income rows per month are allowed; the dashboard sums them.")
@RestController
@RequestMapping("/api/v1/income")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @Operation(summary = "List income rows, optionally filtered to one month")
    @GetMapping
    public List<IncomeResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM; omit for every month", example = "2026-04")
            @RequestParam(required = false) String month) {
        return incomeService.list(principal.id(), MonthUtils.parseOptionalYearMonth(month));
    }

    @Operation(summary = "Book an income row")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                 @Valid @RequestBody IncomeRequest request) {
        return incomeService.create(principal.id(), request);
    }

    @Operation(summary = "Update an income row")
    @ApiResponse(responseCode = "403", description = "The income row belongs to another user")
    @PutMapping("/{id}")
    public IncomeResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(principal.id(), id, request);
    }

    @Operation(summary = "Delete an income row")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        incomeService.delete(principal.id(), id);
    }
}
