package com.financetracker.budget;

import com.financetracker.budget.dto.BudgetGoalRequest;
import com.financetracker.budget.dto.BudgetGoalResponse;
import com.financetracker.budget.dto.BudgetStatusResponse;
import com.financetracker.budget.service.BudgetService;
import com.financetracker.common.util.MonthUtils;
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

@Tag(name = "Budgets", description = "Per-category spend limits and how the month is tracking against them")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "Budget status per category for a month",
            description = "The effective limit is the month-specific goal when one exists, otherwise the "
                    + "recurring goal. WARNING at >= 80% of the limit; EXCEEDED above 100%.")
    @GetMapping
    public List<BudgetStatusResponse> statusForMonth(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM", example = "2026-04", required = true)
            @RequestParam String month) {
        return budgetService.statusForMonth(principal.id(), MonthUtils.parseYearMonth(month));
    }

    @Operation(summary = "List the raw budget goal rows", description = "Use this to find the id for PUT/DELETE.")
    @GetMapping("/goals")
    public List<BudgetGoalResponse> listGoals(@AuthenticationPrincipal AuthPrincipal principal) {
        return budgetService.listGoals(principal.id());
    }

    @Operation(summary = "Create a budget goal",
            description = "Omit `month` for a limit that recurs every month.")
    @ApiResponse(responseCode = "409", description = "A goal already exists for this category and month")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetGoalResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                     @Valid @RequestBody BudgetGoalRequest request) {
        return budgetService.create(principal.id(), request);
    }

    @Operation(summary = "Update a budget goal")
    @ApiResponse(responseCode = "403", description = "The goal belongs to another user")
    @PutMapping("/{id}")
    public BudgetGoalResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody BudgetGoalRequest request) {
        return budgetService.update(principal.id(), id, request);
    }

    @Operation(summary = "Delete a budget goal")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        budgetService.delete(principal.id(), id);
    }
}
