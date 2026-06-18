package com.lastwar.ano2193;

import com.lastwar.ano2193.service.ImageParsingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the raw Tesseract output for ranking-daily-tuesday.png (UUID b176b3fa).
 * Image: Daily Rank / Tues tab — 7 visible ANO entries (ranks 1–7) + rank 23 (Your Alliance).
 *
 * OCR baseline captured with tess4j OEM 1 / PSM 6 from an actual test run.
 * Update EXPECTED_LINES only after a deliberate Tesseract upgrade.
 *
 * Prerequisite: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "ocr.strategy=tesseract")
class OcrRawTextDailyTuesdayIntegrationTest {

    // Trimmed non-empty lines produced by tess4j (OEM 1 / PSM 6) on ranking-daily-tuesday.png.
    // Unicode escapes are used for every non-ASCII character so this baseline is
    // independent of source-file encoding and exactly matches tess4j output.
    // — = EM DASH (—), ” = RIGHT DOUBLE QUOTATION MARK (")
    private static final List<String> EXPECTED_LINES = List.of(
            "RANKING",
            "Daily Rank Weekly Rank",
            "Mon. Tues. Wed. Thur. Fri. Sat.",
            "Ranking Commander Points",
            "ie",
            "— buubeats",
            "8 Ey [ANO] A New Order 10,181,750",
            "5 a",
            "at Nashi224",
            "* fs A New Order bi a ta",
            "Jsu08",
            "8 [ANO] A New Order 9,557,250",
            "= 4,4 FunGe",
            "GB new order 2350750",
            "WhitE WolF 13",
            "7] ol [ANO] A New Order 9,287,375",
            "javalinho",
            "aa ao Mnkdes Ooo”",
            "= Your Alliance"
    );

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void extractRawText_dailyTuesdaySample_matchesKnownOutput() throws Exception {
        assumeTrue(isTesseractAvailable(),
                "Skipping OCR raw-text test — tesseract-ocr not installed. " +
                "Run: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng");

        URL resource = getClass().getResource("/ocr/ranking-daily-tuesday.png");
        assertNotNull(resource, "Test resource /ocr/ranking-daily-tuesday.png not found on classpath");
        File sampleImage = new File(resource.toURI());

        String rawText = imageParsingService.extractRawText(sampleImage);

        List<String> actualLines = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        assertEquals(EXPECTED_LINES, actualLines,
                "OCR output differs from the recorded baseline. " +
                "If this is expected (e.g. tesseract upgrade), update EXPECTED_LINES.");
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