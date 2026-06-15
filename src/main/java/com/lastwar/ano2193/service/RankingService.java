package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.RankingEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RankingService {

    private final RankingEntryRepository repository;

    public RankingService(RankingEntryRepository repository) {
        this.repository = repository;
    }

    public List<RankingEntry> findAll() {
        return repository.findAll();
    }

    public List<RankingEntry> findByCategory(String category) {
        return repository.findByCategory(category);
    }

    public Optional<RankingEntry> findById(Long id) {
        return repository.findById(id);
    }

    public RankingEntry save(RankingEntry entry) {
        return repository.save(entry);
    }

    public void saveAll(List<RankingEntry> entries) {
        repository.saveAll(entries);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    /** Returns sorted list of distinct category names present in the database. */
    public List<String> findAllCategories() {
        return repository.findAll().stream()
                .map(RankingEntry::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
}
