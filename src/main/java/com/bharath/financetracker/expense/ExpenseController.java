package com.bharath.financetracker.expense;

import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.expense.dto.ExpenseRequest;
import com.bharath.financetracker.expense.dto.ExpenseResponse;
import com.bharath.financetracker.expense.dto.ExpensesByCategoryResponse;
import com.bharath.financetracker.expense.service.ExpenseService;
import com.bharath.financetracker.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Expenses")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "A month's expenses grouped by category",
            description = "Each group carries its total, its share of the month's total expense, "
                    + "and the transactions behind it.")
    @GetMapping
    public ExpensesByCategoryResponse listGroupedByCategory(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM", example = "2026-04", required = true)
            @RequestParam String month) {
        return expenseService.listGroupedByCategory(principal.id(), MonthUtils.parseYearMonth(month));
    }

    @Operation(summary = "The flat transaction list, paginated",
            description = "Omit `month` to page across every month.")
    @GetMapping("/transactions")
    public Page<ExpenseResponse> listTransactions(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM; omit for every month", example = "2026-04")
            @RequestParam(required = false) String month,
            @ParameterObject @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return expenseService.listTransactions(principal.id(), MonthUtils.parseOptionalYearMonth(month), pageable);
    }

    @Operation(summary = "Record an expense")
    @ApiResponse(responseCode = "400", description = "categoryId is not an EXPENSE category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(principal.id(), request);
    }

    @Operation(summary = "Update an expense")
    @ApiResponse(responseCode = "403", description = "The expense belongs to another user")
    @PutMapping("/{id}")
    public ExpenseResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(principal.id(), id, request);
    }

    @Operation(summary = "Delete an expense")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        expenseService.delete(principal.id(), id);
    }
}
