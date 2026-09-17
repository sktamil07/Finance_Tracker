package com.financetracker.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyMathTest {

    @Test
    @DisplayName("a zero whole yields 0% instead of dividing by zero")
    void percentageOfZeroWholeIsZero() {
        assertThat(MoneyMath.percentage(new BigDecimal("500.00"), BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(MoneyMath.percentage(new BigDecimal("500.00"), null)).isEqualByComparingTo("0.00");
    }

    @Test
    void percentageRoundsHalfUpToTwoDecimalPlaces() {
        // 130000 / 191000 * 100 = 68.06282722...
        assertThat(MoneyMath.percentage(new BigDecimal("130000.00"), new BigDecimal("191000.00")))
                .isEqualByComparingTo("68.06");
    }

    @Test
    @DisplayName("overspending the salary produces a negative savings percentage, not zero")
    void percentageOfNegativePartIsNegative() {
        assertThat(MoneyMath.percentage(new BigDecimal("-9000.00"), new BigDecimal("90000.00")))
                .isEqualByComparingTo("-10.00");
    }

    @Test
    void nzTreatsNullAsZeroAndNormalisesScale() {
        assertThat(MoneyMath.nz(null)).isEqualByComparingTo("0.00");
        assertThat(MoneyMath.nz(new BigDecimal("5"))).isEqualByComparingTo("5.00");
        assertThat(MoneyMath.nz(new BigDecimal("5.005"))).isEqualByComparingTo("5.01");
    }
}
