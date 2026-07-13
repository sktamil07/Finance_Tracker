package com.bharath.financetracker.investment.service;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.category.service.CategoryService;
import com.bharath.financetracker.common.exception.ResourceNotFoundException;
import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.common.util.MoneyMath;
import com.bharath.financetracker.common.util.OwnershipGuard;
import com.bharath.financetracker.investment.domain.Investment;
import com.bharath.financetracker.investment.dto.InvestmentRequest;
import com.bharath.financetracker.investment.dto.InvestmentResponse;
import com.bharath.financetracker.investment.dto.UpcomingInvestmentResponse;
import com.bharath.financetracker.investment.repository.InvestmentRepository;
import com.bharath.financetracker.mappers.InvestmentMapper;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentServiceImpl implements InvestmentService {

    private static final String RESOURCE = "Investment";

    private final InvestmentRepository investmentRepository;
    private final InvestmentMapper investmentMapper;
    private final CategoryService categoryService;
    private final UserService userService;
    private final Clock clock;

    @Override
    public List<InvestmentResponse> list(Long userId, YearMonth month) {
        List<Investment> rows = month == null
                ? investmentRepository.findByUserIdOrderByMonthDescIdDesc(userId)
                : investmentRepository.findByUserIdAndMonthOrderByIdDesc(userId, month.atDay(1));
        return investmentMapper.toResponseList(rows);
    }

    @Override
    @Transactional
    public InvestmentResponse create(Long userId, InvestmentRequest request) {
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.INVESTMENT);

        Investment investment = Investment.builder()
                .user(userService.requireUser(userId))
                .name(request.name().trim())
                .amount(request.amount())
                .category(category)
                .month(request.month().atDay(1))
                .roiNotes(request.roiNotes())
                .recurring(request.recurring())
                .recurringDayOfMonth(request.recurring() ? request.recurringDayOfMonth() : null)
                .maturityDate(request.maturityDate())
                .build();

        return investmentMapper.toResponse(investmentRepository.save(investment));
    }

    @Override
    @Transactional
    public InvestmentResponse update(Long userId, Long investmentId, InvestmentRequest request) {
        Investment investment = loadOwned(userId, investmentId);
        Category category = categoryService.requireOwned(userId, request.categoryId(), CategoryType.INVESTMENT);

        investment.setName(request.name().trim());
        investment.setAmount(request.amount());
        investment.setCategory(category);
        investment.setMonth(request.month().atDay(1));
        investment.setRoiNotes(request.roiNotes());
        investment.setRecurring(request.recurring());
        investment.setRecurringDayOfMonth(request.recurring() ? request.recurringDayOfMonth() : null);
        investment.setMaturityDate(request.maturityDate());

        return investmentMapper.toResponse(investment);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long investmentId) {
        investmentRepository.delete(loadOwned(userId, investmentId));
    }

    @Override
    public List<UpcomingInvestmentResponse> upcoming(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowEnd = MonthUtils.lastOfMonth(today.plusMonths(1));

        List<UpcomingInvestmentResponse> upcoming = new ArrayList<>();

        // Maturities that have not already passed.
        investmentRepository.findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(userId, today, windowEnd)
                .forEach(i -> upcoming.add(alert(i, UpcomingInvestmentResponse.Kind.MATURITY, i.getMaturityDate())));

        // SIP instalments for recurring investments, one per month in the window.
        for (Investment investment : investmentRepository.findByUserIdAndRecurringTrueOrderByNameAsc(userId)) {
            if (investment.getRecurringDayOfMonth() == null) {
                continue;
            }
            for (YearMonth ym : List.of(YearMonth.from(today), YearMonth.from(today).plusMonths(1))) {
                LocalDate due = sipDateIn(ym, investment.getRecurringDayOfMonth());
                if (!due.isBefore(today) && !due.isAfter(windowEnd)) {
                    upcoming.add(alert(investment, UpcomingInvestmentResponse.Kind.SIP, due));
                }
            }
        }

        upcoming.sort(Comparator.comparing(UpcomingInvestmentResponse::dueDate)
                .thenComparing(UpcomingInvestmentResponse::name));
        return upcoming;
    }

    @Override
    public BigDecimal totalForMonth(Long userId, YearMonth month) {
        return MoneyMath.money(investmentRepository.sumForMonth(userId, month.atDay(1)));
    }

    @Override
    public List<Investment> findForMonth(Long userId, YearMonth month) {
        return investmentRepository.findByUserIdAndMonthOrderByIdDesc(userId, month.atDay(1));
    }

    @Override
    public List<Investment> findMaturingBetween(Long userId, LocalDate from, LocalDate to) {
        return investmentRepository.findByUserIdAndMaturityDateBetweenOrderByMaturityDateAsc(userId, from, to);
    }

    /**
     * A SIP set for the 31st still fires in February — clamp to the month's last day.
     */
    private static LocalDate sipDateIn(YearMonth month, int dayOfMonth) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    private static UpcomingInvestmentResponse alert(Investment investment,
                                                    UpcomingInvestmentResponse.Kind kind,
                                                    LocalDate dueDate) {
        return new UpcomingInvestmentResponse(
                investment.getId(),
                investment.getName(),
                investment.getCategory().getName(),
                investment.getAmount(),
                kind,
                dueDate,
                investment.getRoiNotes());
    }

    private Investment loadOwned(Long userId, Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, investmentId));
        OwnershipGuard.requireOwner(investment.getUser().getId(), userId, RESOURCE, investmentId);
        return investment;
    }
}
