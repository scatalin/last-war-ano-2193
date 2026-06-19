package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.CategoryInstance;
import com.lastwar.ano2193.model.CategoryTag;
import com.lastwar.ano2193.model.UploadCategory;
import com.lastwar.ano2193.repository.CategoryInstanceRepository;
import com.lastwar.ano2193.repository.CategoryTagRepository;
import com.lastwar.ano2193.repository.UploadCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final UploadCategoryRepository repo;
    private final CategoryInstanceRepository instanceRepo;
    private final CategoryTagRepository tagRepo;

    public CategoryService(UploadCategoryRepository repo,
                           CategoryInstanceRepository instanceRepo,
                           CategoryTagRepository tagRepo) {
        this.repo = repo;
        this.instanceRepo = instanceRepo;
        this.tagRepo = tagRepo;
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

    // ── Category tags ─────────────────────────────────────────────────────────

    public List<CategoryTag> findTagsByCategoryId(Long categoryId) {
        return tagRepo.findByCategoryIdOrderByDisplayOrder(categoryId);
    }

    public long countTagsByCategoryId(Long categoryId) {
        return tagRepo.countByCategoryId(categoryId);
    }

    public CategoryTag saveTag(CategoryTag tag) {
        return tagRepo.save(tag);
    }

    public void deleteTag(Long tagId) {
        tagRepo.deleteById(tagId);
    }

    public Optional<CategoryTag> findTagById(Long tagId) {
        return tagRepo.findById(tagId);
    }
}