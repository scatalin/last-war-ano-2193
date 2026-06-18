package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.RankingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.List;

public interface RankingEntryRepository extends JpaRepository<RankingEntry, Long> {

    List<RankingEntry> findByCategory(String category);

    List<RankingEntry> findBySubmittedBy(String submittedBy);

    List<RankingEntry> findByPlayerNameContainingIgnoreCase(String playerName);

    List<RankingEntry> findBySourcePhotoPath(String sourcePhotoPath);

    List<RankingEntry> findBySourcePhotoPathIn(Collection<String> paths);

    @Transactional
    void deleteBySourcePhotoPath(String sourcePhotoPath);
}
