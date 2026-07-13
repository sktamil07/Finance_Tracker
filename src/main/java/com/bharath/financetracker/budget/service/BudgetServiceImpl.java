package com.bharath.financetracker.budget.service;

import com.bharath.financetracker.budget.domain.BudgetGoal;
import com.bharath.financetracker.budget.dto.BudgetGoalRequest;
import com.bharath.financetracker.budget.dto.BudgetGoalResponse;
import com.bharath.financetracker.budget.dto.BudgetStatus;
import com.bharath.financetracker.budget.dto.BudgetStatusResponse;
import com.bharath.financetracker.budget.repository.BudgetGoalRepository;
import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.category.service.CategoryService;
import com.bharath.financetracker.common.exception.ConflictException;
import com.bharath.financetracker.common.exception.ResourceNotFoundException;
import com.bharath.financetracker.common.util.MoneyMath;
import com.bharath.financetracker.common.util.OwnershipGuard;
import com.bharath.financetracker.expense.repository.ExpenseRepository;
import com.bharath.financetracker.mappers.BudgetGoalMapper;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
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
public class BudgetServiceImpl implements BudgetService {

    private static final String RESOURCE = "BudgetGoal";

    private final BudgetGoalRepository budgetGoalRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetGoalMapper budgetGoalMapper;
    private final CategoryService categoryService;
    private final UserService userService;

    @Override
    public List<BudgetStatusResponse> statusForMonth(Long userId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        // Recurring goals first, then let month-specific goals overwrite them per category.
        Map<Long, BudgetGoal> effective = new LinkedHashMap<>();
        budgetGoalRepository.findByUserIdAndMonthIsNull(userId)
                .forEach(goal -> effective.put(goal.getCategory().getId(), goal));
        budgetGoalRepository.findByUserIdAndMonth(userId, monthStart)
                .forEach(goal -> effective.put(goal.getCategory().getId(), goal));

        return effective.values().stream()
                .map(goal -> toStatus(userId, goal, month, monthStart, monthEnd))
                .sorted(Comparator.comparing(BudgetStatusResponse::categoryName))
                .toList();
    }

    @Override
    public List<BudgetGoalResponse> listGoals(Long userId) {
        return budgetGoalMapper.toResponseList(budgetGoalRepository.findByUserIdOrderByIdDesc(userId));
    }

    @Override
    @Transactional
    public BudgetGoalResponse create(Long userId, BudgetGoalRequest request) {
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.EXPENSE);
        LocalDate month = request.month() == null ? null : request.month().atDay(1);

        assertNoDuplicate(userId, category.getId(), month, null);

        BudgetGoal goal = BudgetGoal.builder()
                .user(userService.requireUser(userId))
                .category(category)
                .limitAmount(request.limitAmount())
                .month(month)
                .build();

        return budgetGoalMapper.toResponse(budgetGoalRepository.save(goal));
    }

    @Override
    @Transactional
    public BudgetGoalResponse update(Long userId, Long goalId, BudgetGoalRequest request) {
        BudgetGoal goal = loadOwned(userId, goalId);
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.EXPENSE);
        LocalDate month = request.month() == null ? null : request.month().atDay(1);

        assertNoDuplicate(userId, category.getId(), month, goalId);

        goal.setCategory(category);
        goal.setLimitAmount(request.limitAmount());
        goal.setMonth(month);

        return budgetGoalMapper.toResponse(goal);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long goalId) {
        budgetGoalRepository.delete(loadOwned(userId, goalId));
    }

    private BudgetStatusResponse toStatus(Long userId, BudgetGoal goal, YearMonth month,
                                          LocalDate monthStart, LocalDate monthEnd) {
        Category category = goal.getCategory();
        BigDecimal spent = MoneyMath.money(
                expenseRepository.sumForCategoryAndPeriod(userId, category.getId(), monthStart, monthEnd));
        BigDecimal limit = MoneyMath.money(goal.getLimitAmount());
        BudgetStatus status = BudgetStatusCalculator.evaluate(spent, limit);

        return new BudgetStatusResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                month,
                spent,
                limit,
                MoneyMath.money(limit.subtract(spent)),
                BudgetStatusCalculator.utilisation(spent, limit),
                status,
                goal.getId(),
                goal.getMonth() == null);
    }

    /**
     * The DB unique key on (user, category, month) cannot police the recurring row because MySQL
     * treats NULL months as distinct, so both cases are checked here.
     */
    private void assertNoDuplicate(Long userId, Long categoryId, LocalDate month, Long excludeGoalId) {
        var existing = month == null
                ? budgetGoalRepository.findByUserIdAndCategoryIdAndMonthIsNull(userId, categoryId)
                : budgetGoalRepository.findByUserIdAndCategoryIdAndMonth(userId, categoryId, month);

        existing.filter(goal -> !goal.getId().equals(excludeGoalId))
                .ifPresent(goal -> {
                    throw new ConflictException(month == null
                            ? "A recurring budget goal already exists for this category"
                            : "A budget goal already exists for this category and month");
                });
    }

    private BudgetGoal loadOwned(Long userId, Long goalId) {
        BudgetGoal goal = budgetGoalRepository.findById(goalId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, goalId));
        OwnershipGuard.requireOwner(goal.getUser().getId(), userId, RESOURCE, goalId);
        return goal;
    }
}
