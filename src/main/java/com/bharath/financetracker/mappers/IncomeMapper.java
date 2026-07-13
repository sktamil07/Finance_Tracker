package com.bharath.financetracker.mappers;

import com.bharath.financetracker.income.domain.Income;
import com.bharath.financetracker.income.dto.IncomeResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = TemporalMapper.class)
public interface IncomeMapper {

    IncomeResponse toResponse(Income income);

    List<IncomeResponse> toResponseList(List<Income> incomes);
}
