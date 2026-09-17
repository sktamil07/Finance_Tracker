package com.financetracker.mappers;

import com.financetracker.budget.domain.BudgetGoal;
import com.financetracker.budget.dto.BudgetGoalResponse;
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
