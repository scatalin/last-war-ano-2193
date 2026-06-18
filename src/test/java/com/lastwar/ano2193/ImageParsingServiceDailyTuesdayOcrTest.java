package com.lastwar.ano2193;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.service.ImageParsingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end parser test for ranking-daily-tuesday.png (UUID b176b3fa).
 * Image: Daily Rank / Tues tab, seven visible ANO commanders (ranks 1–7) plus rank 23.
 *
 * The OCR output for this image is noisier than ranking-sample.png — several player
 * names and point values are partially garbled. Tests therefore check for substrings
 * and structural invariants rather than exact values.
 *
 * What the parser extracts from the tess4j output:
 *   rank 1 – "— buubeats"    / ANO / 10,181,750  (rank-1 "Petsi cool" not captured by OCR)
 *   rank 2 – "Jsu08"         / ANO /  9,557,250
 *   rank 3 – "WhitE WolF 13" / ANO /  2,350,750  (FunGe's garbled score bleeds into WolF's entry)
 *
 * Prerequisite: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "ocr.strategy=tesseract")
class ImageParsingServiceDailyTuesdayOcrTest {

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void parseImage_dailyTuesdaySample_extractsKnownEntries() throws Exception {
        assumeTrue(isTesseractAvailable(),
                "Skipping OCR test — tesseract-ocr not installed. " +
                "Run: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng");

        URL resource = getClass().getResource("/ocr/ranking-daily-tuesday.png");
        assertNotNull(resource, "Test resource /ocr/ranking-daily-tuesday.png not found on classpath");
        File sampleImage = new File(resource.toURI());

        List<RankingEntry> entries = imageParsingService.parseImage(sampleImage, "points", "test-user");

        System.out.println("=== OCR extraction results (ranking-daily-tuesday.png) ===");
        entries.forEach(e -> System.out.printf("  rank=%2d  %-25s [%-5s]  points=%,d%n",
                e.getRank(),
                Objects.toString(e.getPlayerName(), "<null>"),
                Objects.toString(e.getAllianceTag(), "?"),
                e.getPower() != null ? e.getPower() : 0));
        System.out.println("==========================================================");

        // Tesseract recovers 3 of the 8 entries from this screenshot
        assertTrue(entries.size() >= 2,
                "Expected at least 2 entries, got " + entries.size());

        // Every entry must have mandatory fields populated
        entries.forEach(e -> {
            assertNotNull(e.getRank(),       "rank must be set on every entry");
            assertEquals("points",    e.getCategory());
            assertEquals("test-user", e.getSubmittedBy());
            assertNotNull(e.getCapturedAt());
        });

        // Ranks are sequential starting from 1
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(i + 1, entries.get(i).getRank(),
                    "Entry at index " + i + " has wrong rank");
        }

        // All extracted entries belong to [ANO]
        entries.forEach(e -> assertEquals("ANO", e.getAllianceTag(),
                "Expected alliance tag ANO for entry: " + e.getPlayerName()));

        // "— buubeats" (rank 2 in the screenshot) is picked up as rank 1 because OCR noise
        // on the rank-1 avatar prevents "Petsi cool" from being parsed as a separate entry
        List<String> names = entries.stream()
                .map(RankingEntry::getPlayerName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .toList();
        assertTrue(names.stream().anyMatch(n -> n.contains("buubeats")),
                "Expected a name containing 'buubeats'; got: " + names);

        // Jsu08 (rank 5 in screenshot) is recovered cleanly
        assertTrue(names.stream().anyMatch(n -> n.contains("jsu")),
                "Expected a name containing 'jsu'; got: " + names);

        // WhitE WolF 13 (rank 7 in screenshot) is recovered
        assertTrue(names.stream().anyMatch(n -> n.contains("wolf")),
                "Expected a name containing 'wolf'; got: " + names);

        // All extracted power values must be positive
        entries.stream()
                .filter(e -> e.getPower() != null)
                .forEach(e -> assertTrue(e.getPower() > 0,
                        "Power for '" + e.getPlayerName() + "' must be positive; got: " + e.getPower()));

        // The two cleanly extracted entries have known exact point values
        entries.stream()
                .filter(e -> e.getPlayerName() != null && e.getPlayerName().toLowerCase().contains("buubeats"))
                .findFirst()
                .ifPresent(e -> assertEquals(10_181_750L, e.getPower(),
                        "buubeats power mismatch"));

        entries.stream()
                .filter(e -> e.getPlayerName() != null && e.getPlayerName().toLowerCase().contains("jsu"))
                .findFirst()
                .ifPresent(e -> assertEquals(9_557_250L, e.getPower(),
                        "Jsu08 power mismatch"));
    }

    private static boolean isTesseractAvailable() {
        try {
            int exit = new ProcessBuilder("tesseract", "--version")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }
}