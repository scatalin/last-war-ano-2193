package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.repository.RankingEntryRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Handles CSV-based persistence of ranking data.
 *
 * <ul>
 *   <li><strong>Startup</strong>  – imports {@code rankings.csv} into the database when
 *       the database is empty (first run or after a reset).</li>
 *   <li><strong>Shutdown</strong> – exports the current database contents to
 *       {@code rankings.csv} via a {@code @PreDestroy} hook.</li>
 *   <li><strong>Periodic</strong> – the {@link com.lastwar.ano2193.scheduler.CsvPersistenceScheduler}
 *       calls {@link #exportRankingsToCsv()} on a configurable interval
 *       (default: every 5 minutes).</li>
 * </ul>
 */
@Service
public class CsvService {

    private static final Logger log = LoggerFactory.getLogger(CsvService.class);
    private static final String RANKINGS_FILENAME = "rankings.csv";
    private static final String[] RANKINGS_HEADER =
            {"id", "rank", "playerName", "allianceTag", "power", "kills",
             "category", "submittedBy", "sourcePhotoPath", "capturedAt"};

    @Value("${app.data.dir:./data}")
    private String dataDir;

    private final RankingEntryRepository rankingEntryRepository;

    public CsvService(RankingEntryRepository rankingEntryRepository) {
        this.rankingEntryRepository = rankingEntryRepository;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @PostConstruct
    public void loadFromCsv() {
        log.debug("loadFromCsv: checking if import needed");
        Path dir = ensureDir();
        if (dir == null) return;

        long existingCount = rankingEntryRepository.count();
        log.trace("loadFromCsv: existingCount={}", existingCount);
        if (existingCount > 0) {
            log.info("Database already contains ranking data – skipping CSV import");
            return;
        }

        Path csvFile = dir.resolve(RANKINGS_FILENAME);
        log.trace("loadFromCsv: csvFile={} exists={}", csvFile, Files.exists(csvFile));
        if (Files.exists(csvFile)) {
            log.info("Importing ranking data from {}", csvFile);
            try {
                importRankingsCsv(csvFile);
            } catch (Exception e) {
                log.error("Failed to import CSV on startup", e);
            }
        }
    }

    @PreDestroy
    public void saveToCsv() {
        log.info("Application shutting down – exporting ranking data to CSV");
        exportRankingsToCsv();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Exports all ranking entries to {@code <app.data.dir>/rankings.csv}.
     * Called by the scheduler and on demand from the admin UI.
     */
    public void exportRankingsToCsv() {
        log.debug("exportRankingsToCsv: starting");
        Path dir = ensureDir();
        if (dir == null) return;

        Path csvFile = dir.resolve(RANKINGS_FILENAME);
        List<RankingEntry> entries = rankingEntryRepository.findAll();
        log.trace("exportRankingsToCsv: csvFile={} entries={}", csvFile, entries.size());
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile.toFile()))) {
            writer.writeNext(RANKINGS_HEADER);
            for (RankingEntry e : entries) {
                log.trace("exportRankingsToCsv: writing id={} playerName={} category={} rank={}",
                        e.getId(), e.getPlayerName(), e.getCategory(), e.getRank());
                writer.writeNext(new String[]{
                        str(e.getId()),
                        str(e.getRank()),
                        e.getPlayerName(),
                        e.getAllianceTag(),
                        str(e.getPower()),
                        str(e.getKills()),
                        e.getCategory(),
                        e.getSubmittedBy(),
                        e.getSourcePhotoPath(),
                        e.getCapturedAt() != null ? e.getCapturedAt().toString() : ""
                });
            }
            log.info("Exported {} ranking entries to {}", entries.size(), csvFile);
        } catch (IOException e) {
            log.error("Failed to export rankings CSV", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void importRankingsCsv(Path csvFile) throws IOException, CsvValidationException {
        log.debug("importRankingsCsv: file={}", csvFile);
        try (CSVReader reader = new CSVReader(new FileReader(csvFile.toFile()))) {
            String[] header = reader.readNext();
            log.trace("importRankingsCsv: header={}", Arrays.toString(header));
            if (header == null) return;

            String[] line;
            int imported = 0;
            while ((line = reader.readNext()) != null) {
                if (line.length < RANKINGS_HEADER.length) {
                    log.trace("importRankingsCsv: skipping short row (columns={}) row={}",
                            line.length, Arrays.toString(line));
                    continue;
                }
                try {
                    RankingEntry entry = new RankingEntry();
                    // Skip line[0] (id) – let JPA auto-generate a new one
                    entry.setRank(parseIntOrNull(line[1]));
                    entry.setPlayerName(nullIfBlank(line[2]));
                    entry.setAllianceTag(nullIfBlank(line[3]));
                    entry.setPower(parseLongOrNull(line[4]));
                    entry.setKills(parseLongOrNull(line[5]));
                    entry.setCategory(nullIfBlank(line[6]));
                    entry.setSubmittedBy(nullIfBlank(line[7]));
                    entry.setSourcePhotoPath(nullIfBlank(line[8]));
                    if (line.length > 9 && !line[9].isBlank()) {
                        entry.setCapturedAt(LocalDateTime.parse(line[9]));
                    }
                    log.trace("importRankingsCsv: parsed rank={} playerName={} category={} power={} kills={}",
                            entry.getRank(), entry.getPlayerName(), entry.getCategory(),
                            entry.getPower(), entry.getKills());
                    rankingEntryRepository.save(entry);
                    imported++;
                } catch (Exception ex) {
                    log.warn("Skipping malformed CSV row: {}", Arrays.toString(line), ex);
                }
            }
            log.info("Imported {} ranking entries from CSV", imported);
        }
    }

    private Path ensureDir() {
        Path dir = Paths.get(dataDir);
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            log.error("Cannot create data directory '{}'", dataDir, e);
            return null;
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static String nullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
