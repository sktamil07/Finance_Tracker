package com.financetracker.income.service;

import com.financetracker.common.exception.ResourceNotFoundException;
import com.financetracker.common.util.MoneyMath;
import com.financetracker.common.util.OwnershipGuard;
import com.financetracker.income.domain.Income;
import com.financetracker.income.dto.IncomeRequest;
import com.financetracker.income.dto.IncomeResponse;
import com.financetracker.income.repository.IncomeRepository;
import com.financetracker.mappers.IncomeMapper;
import com.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeServiceImpl implements IncomeService {

    private static final String RESOURCE = "Income";
    private static final String DEFAULT_SOURCE = "Salary";

    private final IncomeRepository incomeRepository;
    private final IncomeMapper incomeMapper;
    private final UserService userService;

    @Override
    public List<IncomeResponse> list(Long userId, YearMonth month) {
        List<Income> rows = month == null
                ? incomeRepository.findByUserIdOrderByMonthDescIdDesc(userId)
                : incomeRepository.findByUserIdAndMonthOrderByIdDesc(userId, month.atDay(1));
        return incomeMapper.toResponseList(rows);
    }

    @Override
    @Transactional
    public IncomeResponse create(Long userId, IncomeRequest request) {
        Income income = Income.builder()
                .user(userService.requireUser(userId))
                .month(request.month().atDay(1))
                .amount(request.amount())
                .source(sourceOf(request))
                .build();
        return incomeMapper.toResponse(incomeRepository.save(income));
    }

    @Override
    @Transactional
    public IncomeResponse update(Long userId, Long incomeId, IncomeRequest request) {
        Income income = loadOwned(userId, incomeId);
        income.setMonth(request.month().atDay(1));
        income.setAmount(request.amount());
        income.setSource(sourceOf(request));
        return incomeMapper.toResponse(income);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long incomeId) {
        incomeRepository.delete(loadOwned(userId, incomeId));
    }

    @Override
    public BigDecimal totalForMonth(Long userId, YearMonth month) {
        return MoneyMath.money(incomeRepository.sumForMonth(userId, month.atDay(1)));
    }

    private Income loadOwned(Long userId, Long incomeId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, incomeId));
        OwnershipGuard.requireOwner(income.getUser().getId(), userId, RESOURCE, incomeId);
        return income;
    }

    private static String sourceOf(IncomeRequest request) {
        String source = request.source();
        return source == null || source.isBlank() ? DEFAULT_SOURCE : source.trim();
    }
}
