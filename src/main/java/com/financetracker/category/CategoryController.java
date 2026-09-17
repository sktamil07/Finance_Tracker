package com.financetracker.category;

import com.financetracker.category.domain.CategoryType;
import com.financetracker.category.dto.CategoryRequest;
import com.financetracker.category.dto.CategoryResponse;
import com.financetracker.category.dto.CategoryUpdateRequest;
import com.financetracker.category.service.CategoryService;
import com.financetracker.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List categories", description = "Omit `type` to list both EXPENSE and INVESTMENT categories.")
    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                       @RequestParam(required = false) CategoryType type) {
        return categoryService.list(principal.id(), type);
    }

    @Operation(summary = "Create a category")
    @ApiResponse(responseCode = "409", description = "A category with that name and type already exists")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(principal.id(), request);
    }

    @Operation(summary = "Rename a category or change its icon",
            description = "Type is immutable. Renaming reflects everywhere immediately: expenses, "
                    + "investments, and budget goals reference the category by foreign key.")
    @ApiResponse(responseCode = "403", description = "The category belongs to another user")
    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.update(principal.id(), id, request);
    }

    @Operation(summary = "Delete a category",
            description = "Blocked with 409 while any expense, investment, or budget goal still "
                    + "references it. Rows are never silently reassigned.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "409", description = "Category still in use")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        categoryService.delete(principal.id(), id);
    }
}
