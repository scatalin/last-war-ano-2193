package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.RankingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final RankingEntryRepository repository;

    public RankingService(RankingEntryRepository repository) {
        this.repository = repository;
    }

    public List<RankingEntry> findAll() {
        log.debug("findAll");
        List<RankingEntry> result = repository.findAll();
        log.trace("findAll → {} entries", result.size());
        return result;
    }

    public List<RankingEntry> findByCategory(String category) {
        log.debug("findByCategory category={}", category);
        List<RankingEntry> result = repository.findByCategory(category);
        log.trace("findByCategory category={} → {} entries", category, result.size());
        return result;
    }

    public Optional<RankingEntry> findById(Long id) {
        log.debug("findById id={}", id);
        Optional<RankingEntry> result = repository.findById(id);
        log.trace("findById id={} → found={}", id, result.isPresent());
        return result;
    }

    public RankingEntry save(RankingEntry entry) {
        log.debug("save entry playerName={} category={}", entry.getPlayerName(), entry.getCategory());
        RankingEntry saved = repository.save(entry);
        log.trace("save → id={}", saved.getId());
        return saved;
    }

    public void saveAll(List<RankingEntry> entries) {
        log.debug("saveAll count={}", entries.size());
        repository.saveAll(entries);
        log.trace("saveAll complete");
    }

    public void delete(Long id) {
        log.debug("delete id={}", id);
        repository.deleteById(id);
    }

    public void deleteBySourcePhotoPath(String sourcePhotoPath) {
        log.debug("deleteBySourcePhotoPath sourcePhotoPath={}", sourcePhotoPath);
        repository.deleteBySourcePhotoPath(sourcePhotoPath);
    }

    public long count() {
        long n = repository.count();
        log.trace("count → {}", n);
        return n;
    }

    /** Returns sorted list of distinct category names present in the database. */
    public List<String> findAllCategories() {
        log.debug("findAllCategories");
        List<String> categories = repository.findAll().stream()
                .map(RankingEntry::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        log.trace("findAllCategories → {}", categories);
        return categories;
    }
}
