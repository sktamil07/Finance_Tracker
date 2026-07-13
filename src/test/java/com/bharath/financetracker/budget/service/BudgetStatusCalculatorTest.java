package com.bharath.financetracker.budget.service;

import com.bharath.financetracker.budget.dto.BudgetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetStatusCalculatorTest {

    @Nested
    @DisplayName("thresholds")
    class Thresholds {

        @ParameterizedTest(name = "spent {0} of limit {1} -> {2}")
        @CsvSource({
                // Well under the limit.
                "0.00,      10000.00, NONE",
                "5000.00,   10000.00, NONE",

                // Just below the 80% warning line.
                "7999.99,   10000.00, NONE",

                // Exactly 80% is a WARNING (the boundary is inclusive).
                "8000.00,   10000.00, WARNING",
                "8000.01,   10000.00, WARNING",

                // Exactly at the limit is still only a WARNING, not an overrun.
                "10000.00,  10000.00, WARNING",

                // A single paisa over the limit is EXCEEDED. Rounding utilisation to 2dp first
                // would call this 100.00% and wrongly report WARNING.
                "10000.01,  10000.00, EXCEEDED",
                "12500.00,  10000.00, EXCEEDED",
        })
        void evaluatesAgainstTheLimit(BigDecimal spent, BigDecimal limit, BudgetStatus expected) {
            assertThat(BudgetStatusCalculator.evaluate(spent, limit)).isEqualTo(expected);
        }

        @Test
        @DisplayName("80% boundary holds for limits that do not divide evenly")
        void eightyPercentBoundaryOnAwkwardLimit() {
            BigDecimal limit = new BigDecimal("333.33");
            // 80% of 333.33 is 266.664
            assertThat(BudgetStatusCalculator.evaluate(new BigDecimal("266.66"), limit))
                    .isEqualTo(BudgetStatus.NONE);
            assertThat(BudgetStatusCalculator.evaluate(new BigDecimal("266.67"), limit))
                    .isEqualTo(BudgetStatus.WARNING);
        }
    }

    @Nested
    @DisplayName("degenerate limits never divide by zero")
    class DegenerateLimits {

        @Test
        void zeroLimitWithNoSpendIsNone() {
            assertThat(BudgetStatusCalculator.evaluate(BigDecimal.ZERO, BigDecimal.ZERO))
                    .isEqualTo(BudgetStatus.NONE);
        }

        @Test
        void zeroLimitWithAnySpendIsExceeded() {
            assertThat(BudgetStatusCalculator.evaluate(new BigDecimal("0.01"), BigDecimal.ZERO))
                    .isEqualTo(BudgetStatus.EXCEEDED);
        }

        @Test
        void nullLimitWithAnySpendIsExceeded() {
            assertThat(BudgetStatusCalculator.evaluate(new BigDecimal("1.00"), null))
                    .isEqualTo(BudgetStatus.EXCEEDED);
        }

        @Test
        void nullSpendIsTreatedAsZero() {
            assertThat(BudgetStatusCalculator.evaluate(null, new BigDecimal("100.00")))
                    .isEqualTo(BudgetStatus.NONE);
        }

        @Test
        void utilisationOfAZeroLimitIsZeroRatherThanAnArithmeticException() {
            assertThat(BudgetStatusCalculator.utilisation(new BigDecimal("500.00"), BigDecimal.ZERO))
                    .isEqualByComparingTo("0.00");
        }
    }

    @Test
    @DisplayName("utilisation is reported to 2dp")
    void utilisationRoundsToTwoDecimalPlaces() {
        assertThat(BudgetStatusCalculator.utilisation(new BigDecimal("1000.00"), new BigDecimal("3000.00")))
                .isEqualByComparingTo("33.33");
    }
}
