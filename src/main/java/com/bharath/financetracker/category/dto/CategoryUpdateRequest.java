package com.bharath.financetracker.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code type} is intentionally absent: a category's type is immutable once rows reference it.
 * Renaming is safe — expenses and investments point at the category by FK, so the new name shows
 * up everywhere immediately.
 */
@Schema(name = "CategoryUpdateRequest")
public record CategoryUpdateRequest(

        @NotBlank @Size(max = 60) @Schema(example = "Groceries & Food") String name,

        @Size(max = 60) @Schema(nullable = true) String icon
) {
}
