package com.financetracker.budget.service;

import com.financetracker.budget.dto.BudgetStatus;
import com.financetracker.common.util.MoneyMath;

import java.math.BigDecimal;

/**
 * Pure threshold logic, kept out of the service so it can be unit-tested on its own.
 */
public final class BudgetStatusCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WARNING_PERCENT = new BigDecimal("80");

    private BudgetStatusCalculator() {
    }

    /**
     * WARNING at &ge; 80% of the limit, EXCEEDED only once spend goes strictly above 100%.
     * Spending exactly the limit is a WARNING, not an overrun.
     *
     * <p>Comparisons are exact: {@code spent * 100 >= limit * 80} rather than a rounded
     * percentage. Rounding first would report ₹10,000.01 against a ₹10,000 limit as exactly
     * 100.00% and therefore only a WARNING, when it is plainly an overrun.
     *
     * <p>A zero or negative limit cannot be "80% used", so any spend against it is EXCEEDED and no
     * spend at all is NONE. This is also what keeps us from dividing by zero.
     */
    public static BudgetStatus evaluate(BigDecimal spent, BigDecimal limitAmount) {
        BigDecimal safeSpent = MoneyMath.nz(spent);

        if (limitAmount == null || limitAmount.signum() <= 0) {
            return safeSpent.signum() > 0 ? BudgetStatus.EXCEEDED : BudgetStatus.NONE;
        }

        if (safeSpent.compareTo(limitAmount) > 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (safeSpent.multiply(HUNDRED).compareTo(limitAmount.multiply(WARNING_PERCENT)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.NONE;
    }

    /**
     * Reported for display only — never used to decide {@link BudgetStatus}.
     */
    public static BigDecimal utilisation(BigDecimal spent, BigDecimal limitAmount) {
        return MoneyMath.percentage(spent, limitAmount);
    }
}
