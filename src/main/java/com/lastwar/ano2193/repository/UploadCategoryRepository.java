package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.UploadCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadCategoryRepository extends JpaRepository<UploadCategory, Long> {
    Optional<UploadCategory> findByName(String name);
    boolean existsByName(String name);
}