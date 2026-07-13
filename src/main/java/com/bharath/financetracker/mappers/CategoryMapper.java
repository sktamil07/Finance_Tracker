package com.bharath.financetracker.mappers;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.dto.CategoryResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);
}
