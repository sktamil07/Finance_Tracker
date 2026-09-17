package com.financetracker.category.dto;

import com.financetracker.category.domain.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "CategoryRequest")
public record CategoryRequest(

        @NotBlank @Size(max = 60) @Schema(example = "Groceries") String name,

        @NotNull @Schema(example = "EXPENSE") CategoryType type,

        @Size(max = 60) @Schema(example = "shopping-cart", nullable = true) String icon
) {
}
