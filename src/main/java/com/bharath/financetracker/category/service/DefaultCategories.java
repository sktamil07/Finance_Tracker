package com.bharath.financetracker.category.service;

import com.bharath.financetracker.category.domain.CategoryType;

import java.util.List;

/**
 * Seeded for every new user at registration. Marked {@code systemDefault = true} so a UI can
 * distinguish them, but they are ordinary rows the user may rename or delete.
 */
public final class DefaultCategories {

    public static final List<Seed> EXPENSES = List.of(
            new Seed("Food", "utensils", CategoryType.EXPENSE),
            new Seed("Travel", "plane", CategoryType.EXPENSE),
            new Seed("Utilities", "bolt", CategoryType.EXPENSE),
            new Seed("Health", "heart-pulse", CategoryType.EXPENSE),
            new Seed("Entertainment", "film", CategoryType.EXPENSE),
            new Seed("Shopping", "shopping-bag", CategoryType.EXPENSE),
            new Seed("Other", "ellipsis", CategoryType.EXPENSE));

    public static final List<Seed> INVESTMENTS = List.of(
            new Seed("Bonds", "landmark", CategoryType.INVESTMENT),
            new Seed("Mutual Funds", "chart-pie", CategoryType.INVESTMENT),
            new Seed("Stocks", "chart-line", CategoryType.INVESTMENT),
            new Seed("FD", "piggy-bank", CategoryType.INVESTMENT),
            new Seed("PPF", "shield", CategoryType.INVESTMENT),
            new Seed("NPS", "umbrella", CategoryType.INVESTMENT),
            new Seed("Gold", "coins", CategoryType.INVESTMENT),
            new Seed("Crypto", "bitcoin", CategoryType.INVESTMENT));

    private DefaultCategories() {
    }

    public static List<Seed> all() {
        return java.util.stream.Stream.concat(EXPENSES.stream(), INVESTMENTS.stream()).toList();
    }

    public record Seed(String name, String icon, CategoryType type) {
    }
}
