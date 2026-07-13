package com.bharath.financetracker.expense.repository;

import com.bharath.financetracker.expense.domain.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = "category")
    List<Expense> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            Long userId, LocalDate start, LocalDate end);

    /** The flat, paginated transaction list. */
    @EntityGraph(attributePaths = "category")
    Page<Expense> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate start, LocalDate end, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Expense> findByUserId(Long userId, Pageable pageable);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.user.id = :userId and e.transactionDate between :start and :end
            """)
    BigDecimal sumForPeriod(@Param("userId") Long userId,
                            @Param("start") LocalDate start,
                            @Param("end") LocalDate end);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.user.id = :userId
              and e.category.id = :categoryId
              and e.transactionDate between :start and :end
            """)
    BigDecimal sumForCategoryAndPeriod(@Param("userId") Long userId,
                                       @Param("categoryId") Long categoryId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    /**
     * Total spend per category over a window, biggest first. Used by the 6-month report.
     */
    @Query("""
            select new com.bharath.financetracker.expense.repository.CategoryTotal(
                c.id, c.name, c.icon, sum(e.amount))
            from Expense e
            join e.category c
            where e.user.id = :userId and e.transactionDate between :start and :end
            group by c.id, c.name, c.icon
            order by sum(e.amount) desc
            """)
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    boolean existsByCategoryId(Long categoryId);
}
