package com.financetracker.config;

import com.financetracker.budget.domain.BudgetGoal;
import com.financetracker.budget.repository.BudgetGoalRepository;
import com.financetracker.category.domain.Category;
import com.financetracker.category.repository.CategoryRepository;
import com.financetracker.category.service.CategoryService;
import com.financetracker.expense.domain.Expense;
import com.financetracker.expense.repository.ExpenseRepository;
import com.financetracker.income.domain.Income;
import com.financetracker.income.repository.IncomeRepository;
import com.financetracker.investment.domain.Investment;
import com.financetracker.investment.repository.InvestmentRepository;
import com.financetracker.user.domain.User;
import com.financetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads the April 2026 sample data for the {@code dev} profile only.
 *
 * <p>Only <em>raw</em> amounts are seeded — never a percentage, a savings figure, or an allocation
 * split. The dashboard derives all of those at request time, which is why the SOP's sample
 * percentages (which do not reconcile against its own rupee amounts) are not baked in here.
 *
 * <p>Idempotent: it does nothing if the demo user already exists.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEMO_EMAIL = "demo@financetracker.local";
    private static final String DEMO_PASSWORD = "Demo@12345";
    private static final YearMonth APRIL_2026 = YearMonth.of(2026, 4);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final IncomeRepository incomeRepository;
    private final InvestmentRepository investmentRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetGoalRepository budgetGoalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Dev seed skipped: {} already exists", DEMO_EMAIL);
            return;
        }

        User demo = userRepository.save(User.builder()
                .name("Demo User")
                .email(DEMO_EMAIL)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .build());

        categoryService.seedDefaults(demo);

        Map<String, Category> categories = categoryRepository.findByUserIdOrderByNameAsc(demo.getId()).stream()
                .collect(Collectors.toMap(c -> c.getType() + ":" + c.getName(), Function.identity()));

        seedIncome(demo);
        seedInvestments(demo, categories);
        seedExpenses(demo, categories);
        seedBudgetGoals(demo, categories);

        log.info("Dev seed complete. Login as {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    /** The SOP's April 2026 salary. */
    private void seedIncome(User user) {
        incomeRepository.save(Income.builder()
                .user(user)
                .month(APRIL_2026.atDay(1))
                .amount(new BigDecimal("191000.00"))
                .source("Salary")
                .build());
    }

    /**
     * The SOP's four April 2026 investments. Two carry a maturity date so the bond-alert feed and
     * {@code GET /investments/upcoming} have something to show; one is a recurring SIP.
     */
    private void seedInvestments(User user, Map<String, Category> categories) {
        investmentRepository.saveAll(List.of(
                Investment.builder()
                        .user(user)
                        .name("SBI Bluechip SIP")
                        .amount(new BigDecimal("25000.00"))
                        .category(categories.get("INVESTMENT:Mutual Funds"))
                        .month(APRIL_2026.atDay(1))
                        .roiNotes("SIP on the 5th of every month; 12.4% XIRR to date")
                        .recurring(true)
                        .recurringDayOfMonth(5)
                        .build(),
                Investment.builder()
                        .user(user)
                        .name("HDFC Bank Shares")
                        .amount(new BigDecimal("15000.00"))
                        .category(categories.get("INVESTMENT:Stocks"))
                        .month(APRIL_2026.atDay(1))
                        .roiNotes("Long-term hold; no fixed maturity")
                        .build(),
                Investment.builder()
                        .user(user)
                        .name("RBI Floating Rate Bond")
                        .amount(new BigDecimal("50000.00"))
                        .category(categories.get("INVESTMENT:Bonds"))
                        .month(APRIL_2026.atDay(1))
                        .roiNotes("7.35% p.a., payable half-yearly")
                        .maturityDate(LocalDate.of(2026, 5, 18))
                        .build(),
                Investment.builder()
                        .user(user)
                        .name("Bank FD 2024-26")
                        .amount(new BigDecimal("40000.00"))
                        .category(categories.get("INVESTMENT:FD"))
                        .month(APRIL_2026.atDay(1))
                        .roiNotes("7.1% p.a., cumulative")
                        .maturityDate(LocalDate.of(2026, 4, 28))
                        .build()));
    }

    /**
     * Representative April 2026 spend so the expense breakdown, budget status, and 6-month report
     * are not empty on a fresh dev database.
     */
    private void seedExpenses(User user, Map<String, Category> categories) {
        expenseRepository.saveAll(List.of(
                expense(user, categories, "Food", "Monthly groceries", "12400.00", 3),
                expense(user, categories, "Food", "Dining out", "3600.00", 14),
                expense(user, categories, "Travel", "Flight to Bengaluru", "8900.00", 9),
                expense(user, categories, "Utilities", "Electricity bill", "2750.00", 6),
                expense(user, categories, "Utilities", "Broadband", "1199.00", 6),
                expense(user, categories, "Health", "Pharmacy", "1850.00", 17),
                expense(user, categories, "Entertainment", "Streaming subscriptions", "999.00", 1),
                expense(user, categories, "Shopping", "Running shoes", "5400.00", 21),
                expense(user, categories, "Other", "Household help", "4000.00", 2)));
    }

    private void seedBudgetGoals(User user, Map<String, Category> categories) {
        budgetGoalRepository.saveAll(List.of(
                // Recurring: applies to every month unless overridden.
                BudgetGoal.builder()
                        .user(user)
                        .category(categories.get("EXPENSE:Food"))
                        .limitAmount(new BigDecimal("18000.00"))
                        .build(),
                BudgetGoal.builder()
                        .user(user)
                        .category(categories.get("EXPENSE:Shopping"))
                        .limitAmount(new BigDecimal("5000.00"))
                        .build(),
                // Month-specific override for April 2026: a bigger travel allowance.
                BudgetGoal.builder()
                        .user(user)
                        .category(categories.get("EXPENSE:Travel"))
                        .limitAmount(new BigDecimal("10000.00"))
                        .month(APRIL_2026.atDay(1))
                        .build()));
    }

    private static Expense expense(User user, Map<String, Category> categories, String categoryName,
                                   String description, String amount, int dayOfMonth) {
        return Expense.builder()
                .user(user)
                .description(description)
                .amount(new BigDecimal(amount))
                .category(categories.get("EXPENSE:" + categoryName))
                .transactionDate(APRIL_2026.atDay(dayOfMonth))
                .build();
    }
}
