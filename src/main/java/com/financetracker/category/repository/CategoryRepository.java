package com.financetracker.category.repository;

import com.financetracker.category.domain.Category;
import com.financetracker.category.domain.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    boolean existsByUserIdAndNameIgnoreCaseAndType(Long userId, String name, CategoryType type);

    boolean existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(Long userId, String name, CategoryType type, Long id);
}
