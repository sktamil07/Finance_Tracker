package com.bharath.financetracker.expense.service;

import com.bharath.financetracker.expense.domain.Expense;
import com.bharath.financetracker.expense.dto.ExpenseRequest;
import com.bharath.financetracker.expense.dto.ExpenseResponse;
import com.bharath.financetracker.expense.dto.ExpensesByCategoryResponse;
import com.bharath.financetracker.expense.repository.CategoryTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ExpenseService {

    /** The month's expenses grouped by category, with each category's share of the month's total. */
    ExpensesByCategoryResponse listGroupedByCategory(Long userId, YearMonth month);

    /**
     * The flat transaction list, paginated.
     *
     * @param month null spans every month.
     */
    Page<ExpenseResponse> listTransactions(Long userId, YearMonth month, Pageable pageable);

    ExpenseResponse create(Long userId, ExpenseRequest request);

    ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request);

    void delete(Long userId, Long expenseId);

    /** Service-layer only: the month's expense entities, for the dashboard breakdown. */
    List<Expense> findForMonth(Long userId, YearMonth month);

    /** Sum of expenses dated inside the month. Zero when there are none. */
    BigDecimal totalForMonth(Long userId, YearMonth month);

    /** Service-layer only: total spend per category over an inclusive window, biggest first. */
    List<CategoryTotal> totalsByCategory(Long userId, LocalDate start, LocalDate end);
}
