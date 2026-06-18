package com.lastwar.ano2193.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR strategy that sends the image to any OpenAI-compatible vision endpoint
 * (OpenAI GPT-4o, Ollama with a vision model, etc.) and returns the extracted text.
 *
 * Configure via:
 *   ocr.vision.base-url  — e.g. https://api.openai.com  or  http://localhost:11434
 *   ocr.vision.model     — e.g. gpt-4o  or  llava:13b
 *   ocr.vision.api-key   — Bearer token; leave blank for Ollama
 *   ocr.vision.prompt    — instruction sent to the model together with the image
 *   ocr.vision.timeout-seconds
 */
public class VisionLlmOcrStrategy implements OcrStrategy {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final String prompt;
    private final int timeoutSeconds;

    public VisionLlmOcrStrategy(String baseUrl, String model, String apiKey,
                                 String prompt, int timeoutSeconds) {
        this.baseUrl        = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model          = model;
        this.apiKey         = apiKey;
        this.prompt         = prompt;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String extractText(File imageFile) throws OcrException {
        try {
            String dataUrl = buildDataUrl(imageFile);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url",
                                   "image_url", Map.of("url", dataUrl))
                    )
            )));
            body.put("max_tokens", 2048);

            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));

            if (apiKey != null && !apiKey.isBlank()) {
                req.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = HTTP.send(req.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OcrException(
                        "Vision LLM returned HTTP " + response.statusCode()
                        + " from " + baseUrl + ": " + response.body(), null);
            }

            JsonNode root = MAPPER.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText();

        } catch (OcrException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("Vision LLM request interrupted", e);
        } catch (IOException e) {
            throw new OcrException("Vision LLM request failed: " + e.getMessage(), e);
        }
    }

    // LLaVA and most vision models do not benefit from very large images;
    // downscaling to ≤1024px on the long edge drastically cuts inference time on CPU.
    private static final int MAX_DIMENSION = 1024;

    private String buildDataUrl(File imageFile) throws IOException {
        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            // Fallback: send raw bytes (unsupported format)
            byte[] bytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            return "data:" + mimeType(imageFile.getName()) + ";base64,"
                    + Base64.getEncoder().encodeToString(bytes);
        }

        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage img = original;

        if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
            double scale = (double) MAX_DIMENSION / Math.max(w, h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            img = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, nw, nh, null);
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static String mimeType(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".gif"))  return "image/gif";
        if (f.endsWith(".bmp"))  return "image/bmp";
        return "image/png";
    }

    @Override
    public String name() {
        return "vision-llm (" + model + " @ " + baseUrl + ")";
    }
}