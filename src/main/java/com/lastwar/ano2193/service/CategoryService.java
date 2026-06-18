package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.UploadCategory;
import com.lastwar.ano2193.repository.UploadCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final UploadCategoryRepository repo;

    public CategoryService(UploadCategoryRepository repo) {
        this.repo = repo;
    }

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
}