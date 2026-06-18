package com.lastwar.ano2193;

import com.lastwar.ano2193.service.ImageParsingService;
import net.sourceforge.tess4j.TesseractException;
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
 * Verifies that Tesseract produces the expected raw text for the known
 * ranking screenshot. Fails if the OCR engine, tessdata, or image changes
 * in a way that alters the output — making regressions visible before
 * they silently break the parser.
 *
 * Prerequisite: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OcrRawTextIntegrationTest {

    // Raw text produced by Tesseract 5.3.4 on ranking-sample.png.
    // Each line is trimmed; the list is compared against trimmed lines from
    // the actual OCR output so trailing-space differences don't cause failures.
    // Unicode escapes are used for every non-ASCII character so this baseline is
    // independent of source-file encoding and exactly matches tess4j output.
    // — = EM DASH, “ = LEFT DOUBLE QUOTATION MARK, é = é
    private static final List<String> EXPECTED_LINES = List.of(
            "Mon. Tues. Wed. Thur. Fri. Sat.",
            "Ranking Commander Points",
            "P23 (Gen) PHILL",
            "yy Esc is 34,802,316",
            "— Ln [ANO] A New Order",
            "Sy f final d.",
            "Tez “— 32,284,398",
            "ts [BEZT] BADAZZEZ",
            "L—J WEN) SkaterBoi",
            "ey (ie 29,882,900",
            "a4) [ANO] A New Order",
            "Sag) 2Hann",
            "(4) = 29,297,918",
            "=) [ANO] A New Order",
            "Multihunter",
            "B a 27,970,364",
            "idéinex [ANO] A New Order",
            "Sa ee _ ee ae oe",
            "mee javalinho",
            "26 | J 14,595,408",
            "ex L[ANOJA New Order"
    );

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void extractRawText_rankingSample_matchesKnownOutput() throws Exception {
        assumeTrue(isTesseractAvailable(),
                "Skipping OCR raw-text test — tesseract-ocr not installed. " +
                "Run: sudo apt-get install -y tesseract-ocr tesseract-ocr-eng");

        URL resource = getClass().getResource("/ocr/ranking-sample.png");
        assertNotNull(resource, "Test resource /ocr/ranking-sample.png not found on classpath");
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