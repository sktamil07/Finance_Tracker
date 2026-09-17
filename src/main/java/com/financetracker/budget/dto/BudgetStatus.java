package com.financetracker.budget.dto;

/**
 * Thresholds, evaluated against {@code spent / limit}:
 * <ul>
 *   <li>{@code NONE}      — spent &lt; 80% of limit</li>
 *   <li>{@code WARNING}   — spent &ge; 80% and &le; 100% of limit</li>
 *   <li>{@code EXCEEDED}  — spent &gt; 100% of limit</li>
 * </ul>
 * Spending exactly the limit is a WARNING, not EXCEEDED.
 */
public enum BudgetStatus {
    NONE,
    WARNING,
    EXCEEDED
}
