package com.bharath.financetracker.expense.service;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.category.service.CategoryService;
import com.bharath.financetracker.common.exception.ResourceNotFoundException;
import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.common.util.MoneyMath;
import com.bharath.financetracker.common.util.OwnershipGuard;
import com.bharath.financetracker.expense.domain.Expense;
import com.bharath.financetracker.expense.dto.ExpenseCategoryGroup;
import com.bharath.financetracker.expense.dto.ExpenseRequest;
import com.bharath.financetracker.expense.dto.ExpenseResponse;
import com.bharath.financetracker.expense.dto.ExpensesByCategoryResponse;
import com.bharath.financetracker.expense.repository.CategoryTotal;
import com.bharath.financetracker.expense.repository.ExpenseRepository;
import com.bharath.financetracker.mappers.ExpenseMapper;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private static final String RESOURCE = "Expense";

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final CategoryService categoryService;
    private final UserService userService;

    @Override
    public ExpensesByCategoryResponse listGroupedByCategory(Long userId, YearMonth month) {
        List<Expense> expenses = findForMonth(userId, month);

        BigDecimal monthTotal = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, List<Expense>> byCategory = expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getCategory().getId(),
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<ExpenseCategoryGroup> groups = byCategory.values().stream()
                .map(rows -> {
                    Category category = rows.get(0).getCategory();
                    BigDecimal total = rows.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExpenseCategoryGroup(
                            category.getId(),
                            category.getName(),
                            category.getIcon(),
                            MoneyMath.money(total),
                            MoneyMath.percentage(total, monthTotal),
                            expenseMapper.toResponseList(rows));
                })
                .sorted(Comparator.comparing(ExpenseCategoryGroup::total).reversed())
                .toList();

        return new ExpensesByCategoryResponse(month, MoneyMath.money(monthTotal), groups);
    }

    @Override
    public Page<ExpenseResponse> listTransactions(Long userId, YearMonth month, Pageable pageable) {
        Page<Expense> page = month == null
                ? expenseRepository.findByUserId(userId, pageable)
                : expenseRepository.findByUserIdAndTransactionDateBetween(
                userId, month.atDay(1), month.atEndOfMonth(), pageable);
        return page.map(expenseMapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse create(Long userId, ExpenseRequest request) {
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.EXPENSE);

        Expense expense = Expense.builder()
                .user(userService.requireUser(userId))
                .description(request.description().trim())
                .amount(request.amount())
                .category(category)
                .transactionDate(request.transactionDate())
                .notes(request.notes())
                .build();

        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = loadOwned(userId, expenseId);
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.EXPENSE);

        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setCategory(category);
        expense.setTransactionDate(request.transactionDate());
        expense.setNotes(request.notes());

        return expenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long expenseId) {
        expenseRepository.delete(loadOwned(userId, expenseId));
    }

    @Override
    public List<Expense> findForMonth(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = MonthUtils.lastOfMonth(start);
        return expenseRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(userId, start, end);
    }

    @Override
    public BigDecimal totalForMonth(Long userId, YearMonth month) {
        return MoneyMath.money(expenseRepository.sumForPeriod(userId, month.atDay(1), month.atEndOfMonth()));
    }

    @Override
    public List<CategoryTotal> totalsByCategory(Long userId, LocalDate start, LocalDate end) {
        return expenseRepository.totalsByCategory(userId, start, end);
    }

    private Expense loadOwned(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, expenseId));
        OwnershipGuard.requireOwner(expense.getUser().getId(), userId, RESOURCE, expenseId);
        return expense;
    }
}
