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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the raw gpt-4o output for ranking-daily-tuesday.png.
 *
 * Run once to capture the baseline, then replace EXPECTED_LINES with the actual output.
 * Requires the OCR_OPENAI_API_KEY environment variable to be set and billing active.
 *
 * Quick terminal check before running:
 *   curl -s https://api.openai.com/v1/models -H "Authorization: Bearer $OCR_OPENAI_API_KEY" | head -c 200
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "ocr.strategy=openai",
                "ocr.openai.model=gpt-4o"
        })
class OcrRawTextDailyTuesdayOpenAiIntegrationTest {

    // Baseline captured from gpt-4o on ranking-daily-tuesday.png (2026-06-19).
    // Update only after a deliberate model upgrade or prompt change.
    // Code fences are stripped by VisionLlmOcrStrategy before this baseline is compared.
    private static final List<String> EXPECTED_LINES = List.of(
            "1",
            "Petsi cool",
            "[ANO] A New Order",
            "15,285,875",
            "2",
            "buubeats",
            "[ANO] A New Order",
            "10,181,750",
            "3",
            "KillerKiwiBird",
            "[ANO] A New Order",
            "9,754,000",
            "4",
            "Nash1224",
            "[ANO] A New Order",
            "9,650,500",
            "5",
            "Jsu08",
            "[ANO] A New Order",
            "9,557,250",
            "6",
            "FunGe",
            "[ANO] A New Order",
            "9,350,750",
            "7",
            "WhiTe WOlf 13",
            "[ANO] A New Order",
            "9,287,375",
            "23",
            "javalinho",
            "[ANO]A New Order",
            "8,139,500"
    );

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void extractRawText_dailyTuesdaySample_matchesKnownOutput() throws Exception {
        String apiKey = System.getenv("OCR_OPENAI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Skipping OpenAI test — OCR_OPENAI_API_KEY env var not set.");
        assumeTrue(isOpenAiReachable(apiKey),
                "Skipping OpenAI test — API key invalid or billing not active " +
                "(run: curl -s https://api.openai.com/v1/models -H \"Authorization: Bearer $OCR_OPENAI_API_KEY\" | head -c 200)");

        URL resource = getClass().getResource("/ocr/ranking-daily-tuesday.png");
        assertNotNull(resource, "Test resource /ocr/ranking-daily-tuesday.png not found on classpath");
        File sampleImage = new File(resource.toURI());

        String rawText = imageParsingService.extractRawText(sampleImage);
        assertFalse(rawText == null || rawText.isBlank(), "gpt-4o returned blank text");

        List<String> actualLines = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        System.out.println("\n=== gpt-4o RAW OUTPUT for ranking-daily-tuesday.png ===");
        actualLines.forEach(System.out::println);
        System.out.println("=======================================================\n");

        assertEquals(EXPECTED_LINES, actualLines,
                "gpt-4o output differs from the recorded baseline. " +
                "If this is expected (model upgrade or prompt change), update EXPECTED_LINES.");
    }

    private static boolean isOpenAiReachable(String apiKey) {
        try {
            int code = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                                  .uri(java.net.URI.create("https://api.openai.com/v1/models"))
                                  .header("Authorization", "Bearer " + apiKey)
                                  .GET().build(),
                          java.net.http.HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}