package com.bharath.financetracker.investment.service;

import com.bharath.financetracker.investment.domain.Investment;
import com.bharath.financetracker.investment.dto.InvestmentRequest;
import com.bharath.financetracker.investment.dto.InvestmentResponse;
import com.bharath.financetracker.investment.dto.UpcomingInvestmentResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface InvestmentService {

    /** @param month null lists every investment the user owns, newest month first. */
    List<InvestmentResponse> list(Long userId, YearMonth month);

    InvestmentResponse create(Long userId, InvestmentRequest request);

    InvestmentResponse update(Long userId, Long investmentId, InvestmentRequest request);

    void delete(Long userId, Long investmentId);

    /**
     * SIP instalments and maturities falling between today and the end of next month.
     */
    List<UpcomingInvestmentResponse> upcoming(Long userId);

    /** Sum of every investment booked for the month. Zero when there are none. */
    BigDecimal totalForMonth(Long userId, YearMonth month);

    /** Service-layer only: the month's investment entities, for the dashboard breakdown. */
    List<Investment> findForMonth(Long userId, YearMonth month);

    /** Service-layer only: investments maturing inside an inclusive window, for bond alerts. */
    List<Investment> findMaturingBetween(Long userId, LocalDate from, LocalDate to);
}
