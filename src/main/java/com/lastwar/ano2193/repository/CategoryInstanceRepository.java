package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.CategoryInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryInstanceRepository extends JpaRepository<CategoryInstance, Long> {
    List<CategoryInstance> findByCategoryIdOrderByStartDateDesc(Long categoryId);
}