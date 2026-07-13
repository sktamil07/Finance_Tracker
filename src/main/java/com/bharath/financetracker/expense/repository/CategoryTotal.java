package com.bharath.financetracker.expense.repository;

import java.math.BigDecimal;

/**
 * JPQL constructor-expression target for "total spend per category over a window".
 */
public record CategoryTotal(Long categoryId, String categoryName, String categoryIcon, BigDecimal total) {
}
