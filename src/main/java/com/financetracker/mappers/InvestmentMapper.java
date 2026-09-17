package com.financetracker.mappers;

import com.financetracker.investment.domain.Investment;
import com.financetracker.investment.dto.InvestmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = TemporalMapper.class)
public interface InvestmentMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryIcon", source = "category.icon")
    InvestmentResponse toResponse(Investment investment);

    List<InvestmentResponse> toResponseList(List<Investment> investments);
}
