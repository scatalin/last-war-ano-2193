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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the raw LLaVA:13b (Ollama) output for ranking-daily-tuesday.png.
 *
 * Run once to capture the baseline, then replace EXPECTED_LINES with the actual output.
 * Requires Ollama running locally with llava:13b pulled:
 *   docker run -d -p 11434:11434 -v ollama-data:/root/.ollama ollama/ollama
 *   docker exec -it ollama ollama pull llava:13b
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "ocr.strategy=vision-llm",
                "ocr.vision.base-url=http://localhost:11434",
                "ocr.vision.model=llava:13b",
                "ocr.vision.api-key=",
                "ocr.vision.timeout-seconds=300"
        })
class OcrRawTextDailyTuesdayVisionLlmIntegrationTest {

    // Baseline captured from llava:13b on ranking-daily-tuesday.png.
    // Update only after a deliberate model upgrade or prompt change.
    // TODO: replace with actual output after first discovery run.
    private static final List<String> EXPECTED_LINES = List.of(
            "DISCOVERY_RUN — replace this list with the actual output printed below"
    );

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void extractRawText_dailyTuesdaySample_matchesKnownOutput() throws Exception {
        assumeTrue(isOllamaAvailable(),
                "Skipping vision-llm test — Ollama not reachable at http://localhost:11434. " +
                "Start Ollama and pull llava:13b before running this test.");

        URL resource = getClass().getResource("/ocr/ranking-daily-tuesday.png");
        assertNotNull(resource, "Test resource /ocr/ranking-daily-tuesday.png not found on classpath");
        File sampleImage = new File(resource.toURI());

        String rawText = imageParsingService.extractRawText(sampleImage);
        assertFalse(rawText == null || rawText.isBlank(), "LLaVA returned blank text");

        List<String> actualLines = Arrays.stream(rawText.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        System.out.println("\n=== LLaVA:13b RAW OUTPUT for ranking-daily-tuesday.png ===");
        actualLines.forEach(System.out::println);
        System.out.println("============================================================");
        System.out.println("Java list literal for EXPECTED_LINES:");
        actualLines.forEach(l -> System.out.println("        \"" + l.replace("\\", "\\\\").replace("\"", "\\\"") + "\","));
        System.out.println("============================================================\n");
    }

    private static boolean isOllamaAvailable() {
        try {
            int code = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                                  .uri(java.net.URI.create("http://localhost:11434/api/tags"))
                                  .GET().build(),
                          java.net.http.HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}