package com.bharath.financetracker.budget.repository;

import com.bharath.financetracker.budget.domain.BudgetGoal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, Long> {

    /** Month-specific overrides for a given month. */
    @EntityGraph(attributePaths = "category")
    List<BudgetGoal> findByUserIdAndMonth(Long userId, LocalDate month);

    /** The recurring ("every month") limits. */
    @EntityGraph(attributePaths = "category")
    List<BudgetGoal> findByUserIdAndMonthIsNull(Long userId);

    @EntityGraph(attributePaths = "category")
    List<BudgetGoal> findByUserIdOrderByIdDesc(Long userId);

    Optional<BudgetGoal> findByUserIdAndCategoryIdAndMonth(Long userId, Long categoryId, LocalDate month);

    Optional<BudgetGoal> findByUserIdAndCategoryIdAndMonthIsNull(Long userId, Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
