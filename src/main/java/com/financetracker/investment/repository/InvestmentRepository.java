package com.financetracker.investment.repository;

import com.financetracker.investment.domain.Investment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    @EntityGraph(attributePaths = "category")
    List<Investment> findByUserIdAndMonthOrderByIdDesc(Long userId, LocalDate month);

    @EntityGraph(attributePaths = "category")
    List<Investment> findByUserIdOrderByMonthDescIdDesc(Long userId);

    @Query("""
            select coalesce(sum(i.amount), 0)
            from Investment i
            where i.user.id = :userId and i.month = :month
            """)
    BigDecimal sumForMonth(@Param("userId") Long userId, @Param("month") LocalDate month);

    /**
     * Investments maturing inside an inclusive date window — the bond/maturity alert feed.
     */
    @EntityGraph(attributePaths = "category")
    List<Investment> findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(
            Long userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "category")
    List<Investment> findByUserIdAndRecurringTrueOrderByNameAsc(Long userId);

    boolean existsByCategoryId(Long categoryId);
}
