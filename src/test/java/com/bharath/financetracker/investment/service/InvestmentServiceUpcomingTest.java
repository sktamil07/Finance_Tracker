package com.bharath.financetracker.investment.service;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.category.service.CategoryService;
import com.bharath.financetracker.investment.domain.Investment;
import com.bharath.financetracker.investment.dto.UpcomingInvestmentResponse;
import com.bharath.financetracker.investment.dto.UpcomingInvestmentResponse.Kind;
import com.bharath.financetracker.investment.repository.InvestmentRepository;
import com.bharath.financetracker.mappers.InvestmentMapper;
import com.bharath.financetracker.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

/**
 * The upcoming feed depends on "today", so the clock is pinned to 20 April 2026.
 * The window therefore runs 2026-04-20 .. 2026-05-31.
 */
@ExtendWith(MockitoExtension.class)
class InvestmentServiceUpcomingTest {

    private static final Long USER_ID = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 20);
    private static final LocalDate WINDOW_END = LocalDate.of(2026, 5, 31);

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private InvestmentMapper investmentMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private UserService userService;

    private InvestmentServiceImpl investmentService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        investmentService = new InvestmentServiceImpl(
                investmentRepository, investmentMapper, categoryService, userService, fixed);
    }

    @Test
    @DisplayName("maturities and SIP instalments inside the window are returned, ordered by due date")
    void returnsMaturitiesAndSipsOrderedByDueDate() {
        Category bonds = category(10L, "Bonds", CategoryType.INVESTMENT);
        Category mutualFunds = category(11L, "Mutual Funds", CategoryType.INVESTMENT);

        when(investmentRepository.findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(
                USER_ID, TODAY, WINDOW_END))
                .thenReturn(List.of(maturing(1L, "RBI Bond", bonds, LocalDate.of(2026, 5, 18))));

        when(investmentRepository.findByUserIdAndRecurringTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(sip(2L, "Bluechip SIP", mutualFunds, 5)));

        List<UpcomingInvestmentResponse> upcoming = investmentService.upcoming(USER_ID);

        // The 5 April instalment already passed; only 5 May is upcoming.
        assertThat(upcoming)
                .extracting(UpcomingInvestmentResponse::name, UpcomingInvestmentResponse::kind,
                        UpcomingInvestmentResponse::dueDate)
                .containsExactly(
                        tuple("Bluechip SIP", Kind.SIP, LocalDate.of(2026, 5, 5)),
                        tuple("RBI Bond", Kind.MATURITY, LocalDate.of(2026, 5, 18)));
    }

    @Test
    @DisplayName("a SIP set for the 31st is clamped to the last day of a shorter month")
    void sipDayIsClampedToMonthLength() {
        Category mutualFunds = category(11L, "Mutual Funds", CategoryType.INVESTMENT);

        when(investmentRepository.findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(
                USER_ID, TODAY, WINDOW_END))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndRecurringTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(sip(2L, "Month-end SIP", mutualFunds, 31)));

        List<UpcomingInvestmentResponse> upcoming = investmentService.upcoming(USER_ID);

        // April has 30 days, so the April instalment lands on the 30th; May keeps the 31st.
        assertThat(upcoming).extracting(UpcomingInvestmentResponse::dueDate)
                .containsExactly(LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("a recurring investment with no SIP day contributes nothing rather than throwing")
    void recurringWithoutDayIsSkipped() {
        Category mutualFunds = category(11L, "Mutual Funds", CategoryType.INVESTMENT);

        when(investmentRepository.findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(
                USER_ID, TODAY, WINDOW_END))
                .thenReturn(List.of());
        when(investmentRepository.findByUserIdAndRecurringTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(sip(2L, "Broken SIP", mutualFunds, null)));

        assertThat(investmentService.upcoming(USER_ID)).isEmpty();
    }

    private static Category category(Long id, String name, CategoryType type) {
        return Category.builder().id(id).name(name).type(type).build();
    }

    private static Investment maturing(Long id, String name, Category category, LocalDate maturityDate) {
        return base(id, name, category).maturityDate(maturityDate).build();
    }

    private static Investment sip(Long id, String name, Category category, Integer dayOfMonth) {
        return base(id, name, category).recurring(true).recurringDayOfMonth(dayOfMonth).build();
    }

    private static Investment.InvestmentBuilder base(Long id, String name, Category category) {
        return Investment.builder()
                .id(id)
                .name(name)
                .amount(new BigDecimal("25000.00"))
                .category(category)
                .month(YearMonth.of(2026, 4).atDay(1));
    }
}
