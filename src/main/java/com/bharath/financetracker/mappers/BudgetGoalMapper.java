package com.bharath.financetracker.mappers;

import com.bharath.financetracker.budget.domain.BudgetGoal;
import com.bharath.financetracker.budget.dto.BudgetGoalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = TemporalMapper.class)
public interface BudgetGoalMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "recurring", expression = "java(goal.getMonth() == null)")
    BudgetGoalResponse toResponse(BudgetGoal goal);

    List<BudgetGoalResponse> toResponseList(List<BudgetGoal> goals);
}
