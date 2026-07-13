package com.bharath.financetracker.category.service;

import com.bharath.financetracker.budget.repository.BudgetGoalRepository;
import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.category.dto.CategoryRequest;
import com.bharath.financetracker.category.dto.CategoryResponse;
import com.bharath.financetracker.category.dto.CategoryUpdateRequest;
import com.bharath.financetracker.category.repository.CategoryRepository;
import com.bharath.financetracker.common.exception.BadRequestException;
import com.bharath.financetracker.common.exception.ConflictException;
import com.bharath.financetracker.common.exception.ResourceNotFoundException;
import com.bharath.financetracker.common.util.OwnershipGuard;
import com.bharath.financetracker.expense.repository.ExpenseRepository;
import com.bharath.financetracker.investment.repository.InvestmentRepository;
import com.bharath.financetracker.mappers.CategoryMapper;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private static final String RESOURCE = "Category";

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final InvestmentRepository investmentRepository;
    private final BudgetGoalRepository budgetGoalRepository;
    private final CategoryMapper categoryMapper;
    private final UserService userService;

    @Override
    public List<CategoryResponse> list(Long userId, CategoryType type) {
        List<Category> categories = type == null
                ? categoryRepository.findByUserIdOrderByNameAsc(userId)
                : categoryRepository.findByUserIdAndTypeOrderByNameAsc(userId, type);
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, request.name(), request.type())) {
            throw new ConflictException(
                    "A %s category named '%s' already exists".formatted(request.type(), request.name()));
        }

        Category category = Category.builder()
                .user(userService.requireUser(userId))
                .name(request.name().trim())
                .type(request.type())
                .icon(request.icon())
                .systemDefault(false)
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request) {
        Category category = loadOwned(userId, categoryId);

        String newName = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                userId, newName, category.getType(), categoryId)) {
            throw new ConflictException(
                    "A %s category named '%s' already exists".formatted(category.getType(), newName));
        }

        category.setName(newName);
        category.setIcon(request.icon());
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = loadOwned(userId, categoryId);
        assertNotInUse(category);
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public void seedDefaults(User user) {
        List<Category> seeds = DefaultCategories.all().stream()
                .map(seed -> Category.builder()
                        .user(user)
                        .name(seed.name())
                        .icon(seed.icon())
                        .type(seed.type())
                        .systemDefault(true)
                        .build())
                .toList();
        categoryRepository.saveAll(seeds);
    }

    @Override
    public Category requireOwned(Long userId, Long categoryId, CategoryType expectedType) {
        Category category = loadOwned(userId, categoryId);
        if (expectedType != null && category.getType() != expectedType) {
            throw new BadRequestException("Category %d is of type %s; this endpoint requires %s"
                    .formatted(categoryId, category.getType(), expectedType));
        }
        return category;
    }

    private Category loadOwned(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, categoryId));
        OwnershipGuard.requireOwner(category.getUser().getId(), userId, RESOURCE, categoryId);
        return category;
    }

    private void assertNotInUse(Category category) {
        Long id = category.getId();
        if (expenseRepository.existsByCategoryId(id)
                || investmentRepository.existsByCategoryId(id)
                || budgetGoalRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    ("Category '%s' is still in use by expenses, investments, or budget goals. "
                            + "Move or delete those rows first.").formatted(category.getName()));
        }
    }
}
