package com.financetracker.mappers;

import com.financetracker.income.domain.Income;
import com.financetracker.income.dto.IncomeResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = TemporalMapper.class)
public interface IncomeMapper {

    IncomeResponse toResponse(Income income);

    List<IncomeResponse> toResponseList(List<Income> incomes);
}
