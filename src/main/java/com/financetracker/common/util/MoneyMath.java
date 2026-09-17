package com.financetracker.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money and percentage arithmetic. Every derived figure in the app is computed through here so
 * rounding and divide-by-zero behave identically everywhere.
 */
public final class MoneyMath {

    public static final int MONEY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private MoneyMath() {
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal money(BigDecimal value) {
        return nz(value);
    }

    /**
     * {@code part / whole * 100}, rounded to 2dp. Returns 0 when {@code whole} is zero or null —
     * we never divide by zero, and a 0% share of nothing is the honest answer.
     */
    public static BigDecimal percentage(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, ROUNDING);
        }
        return nz(part).multiply(HUNDRED).divide(whole, PERCENT_SCALE, ROUNDING);
    }
}
