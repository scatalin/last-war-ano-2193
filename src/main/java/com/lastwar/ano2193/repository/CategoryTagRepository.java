package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.CategoryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryTagRepository extends JpaRepository<CategoryTag, Long> {
    List<CategoryTag> findByCategoryIdOrderByDisplayOrder(Long categoryId);
    long countByCategoryId(Long categoryId);
}