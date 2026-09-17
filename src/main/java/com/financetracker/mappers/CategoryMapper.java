package com.financetracker.mappers;

import com.financetracker.category.domain.Category;
import com.financetracker.category.dto.CategoryResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);
}
