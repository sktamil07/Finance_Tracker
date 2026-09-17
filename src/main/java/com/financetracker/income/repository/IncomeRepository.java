package com.financetracker.income.repository;

import com.financetracker.income.domain.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUserIdOrderByMonthDescIdDesc(Long userId);

    List<Income> findByUserIdAndMonthOrderByIdDesc(Long userId, LocalDate month);

    /**
     * Total salary/income booked for the month. Zero when the user booked nothing.
     */
    @Query("""
            select coalesce(sum(i.amount), 0)
            from Income i
            where i.user.id = :userId and i.month = :month
            """)
    BigDecimal sumForMonth(@Param("userId") Long userId, @Param("month") LocalDate month);
}
