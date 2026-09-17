package com.financetracker.mappers;

import com.financetracker.expense.domain.Expense;
import com.financetracker.expense.dto.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface ExpenseMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryIcon", source = "category.icon")
    ExpenseResponse toResponse(Expense expense);

    List<ExpenseResponse> toResponseList(List<Expense> expenses);
}
