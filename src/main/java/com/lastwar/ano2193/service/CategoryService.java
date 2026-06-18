package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.CategoryInstance;
import com.lastwar.ano2193.model.UploadCategory;
import com.lastwar.ano2193.repository.CategoryInstanceRepository;
import com.lastwar.ano2193.repository.UploadCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final UploadCategoryRepository repo;
    private final CategoryInstanceRepository instanceRepo;

    public CategoryService(UploadCategoryRepository repo, CategoryInstanceRepository instanceRepo) {
        this.repo = repo;
        this.instanceRepo = instanceRepo;
    }

    // ── Category definitions ──────────────────────────────────────────────────

    public List<UploadCategory> findAll() {
        return repo.findAll();
    }

    public Optional<UploadCategory> findById(Long id) {
        return repo.findById(id);
    }

    public boolean existsByName(String name) {
        return repo.existsByName(name);
    }

    public UploadCategory save(UploadCategory category) {
        return repo.save(category);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void createIfAbsent(String name, String description) {
        if (!repo.existsByName(name)) {
            UploadCategory c = new UploadCategory();
            c.setName(name);
            c.setDescription(description);
            repo.save(c);
        }
    }

    // ── Category instances ────────────────────────────────────────────────────

    public List<CategoryInstance> findAllInstances() {
        return instanceRepo.findAll();
    }

    public Optional<CategoryInstance> findInstanceById(Long id) {
        return instanceRepo.findById(id);
    }

    public CategoryInstance saveInstance(CategoryInstance instance) {
        return instanceRepo.save(instance);
    }

    public void deleteInstance(Long id) {
        instanceRepo.deleteById(id);
    }
}