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
 * End-to-end OCR test using the real Tesseract engine and the sample ranking
 * screenshot bundled under src/test/resources/ocr/ranking-sample.png.
 *
 * Prerequisites (skipped gracefully if absent):
 *   sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ImageParsingServiceOcrTest {

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void parseImage_rankingSample_extractsKnownEntries() throws Exception {
        assumeTrue(isTesseractAvailable(),
                "Skipping OCR test — tesseract-ocr not installed. " +
                "Run: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng");

        URL resource = getClass().getResource("/ocr/ranking-sample.png");
        assertNotNull(resource, "Test resource /ocr/ranking-sample.png not found on classpath");
        File sampleImage = new File(resource.toURI());

        List<RankingEntry> entries = imageParsingService.parseImage(sampleImage, "points", "test-user");

        // Print what Tesseract actually extracted so the output is visible in test logs
        System.out.println("=== OCR extraction results ===");
        entries.forEach(e -> System.out.printf("  rank=%2d  %-22s [%-5s]  points=%,d%n",
                e.getRank(),
                Objects.toString(e.getPlayerName(), "<null>"),
                Objects.toString(e.getAllianceTag(), "?"),
                e.getPower() != null ? e.getPower() : 0));
        System.out.println("==============================");

        // Screenshot contains at least 5 visible entries (PHiLL, final day, SkaterBoi, 2Hann, Multihunter)
        assertTrue(entries.size() >= 3,
                "Expected at least 3 entries, got " + entries.size());

        // Every entry must have mandatory fields
        entries.forEach(e -> {
            assertNotNull(e.getRank(),     "rank must be set on every entry");
            assertEquals("points", e.getCategory());
            assertEquals("test-user", e.getSubmittedBy());
            assertNotNull(e.getCapturedAt());
        });

        // Ranks must be sequential starting from 1
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(i + 1, entries.get(i).getRank(),
                    "Entry at index " + i + " has wrong rank");
        }

        // Player names from game screenshots are often prefixed with OCR noise from
        // avatar graphics (e.g. "P23 (Gen) PHILL" instead of "PHiLL"), so we check
        // for a substring match rather than an exact name.
        List<String> names = entries.stream()
                .map(RankingEntry::getPlayerName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .toList();
        assertTrue(names.stream().anyMatch(n -> n.contains("phill")),
                "Expected a name containing 'phill' (rank-1 player); got: " + names);

        // Alliance [ANO] appears multiple times in the screenshot
        List<String> tags = entries.stream()
                .map(RankingEntry::getAllianceTag)
                .filter(Objects::nonNull)
                .toList();
        assertTrue(tags.stream().anyMatch(t -> t.equalsIgnoreCase("ANO")),
                "Expected [ANO] among extracted alliance tags; got: " + tags);

        // Point values for this screenshot are in the tens of millions
        entries.stream()
                .filter(e -> e.getPower() != null)
                .forEach(e -> assertTrue(e.getPower() > 1_000_000,
                        "Points for '" + e.getPlayerName() + "' should be > 1M, got: " + e.getPower()));
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