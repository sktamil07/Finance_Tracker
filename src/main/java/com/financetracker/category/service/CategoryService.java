package com.financetracker.category.service;

import com.financetracker.category.domain.Category;
import com.financetracker.category.domain.CategoryType;
import com.financetracker.category.dto.CategoryRequest;
import com.financetracker.category.dto.CategoryResponse;
import com.financetracker.category.dto.CategoryUpdateRequest;
import com.financetracker.user.domain.User;

import java.util.List;

public interface CategoryService {

    /**
     * @param type null returns every category the user owns.
     */
    List<CategoryResponse> list(Long userId, CategoryType type);

    CategoryResponse create(Long userId, CategoryRequest request);

    CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request);

    /**
     * Blocks the delete with 409 when any expense, investment, or budget goal still references
     * the category. Reassignment is deliberately not offered — silently moving a user's history
     * into another bucket is worse than making them do it explicitly.
     */
    void delete(Long userId, Long categoryId);

    /** Service-layer only. Seeds the SOP default categories for a freshly registered user. */
    void seedDefaults(User user);

    /**
     * Service-layer only. Resolves a category, enforcing ownership (403) and that its type matches
     * what the calling endpoint requires (400).
     */
    Category requireOwned(Long userId, Long categoryId, CategoryType expectedType);
}
