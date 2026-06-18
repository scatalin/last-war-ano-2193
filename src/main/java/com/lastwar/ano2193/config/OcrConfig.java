package com.lastwar.ano2193.config;

import com.lastwar.ano2193.ocr.MockOcrStrategy;
import com.lastwar.ano2193.ocr.OcrStrategy;
import com.lastwar.ano2193.ocr.TesseractOcrStrategy;
import com.lastwar.ano2193.ocr.VisionLlmOcrStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfig {

    private static final Logger log = LoggerFactory.getLogger(OcrConfig.class);

    @Value("${ocr.strategy:tesseract}")
    private String strategyName;

    // ── Tesseract ─────────────────────────────────────────────────────────────
    @Value("${ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}")
    private String tessDataPath;

    // ── Vision LLM (OpenAI-compatible) ───────────────────────────────────────
    @Value("${ocr.vision.base-url:http://localhost:11434}")
    private String visionBaseUrl;

    @Value("${ocr.vision.model:llava:13b}")
    private String visionModel;

    @Value("${ocr.vision.api-key:}")
    private String visionApiKey;

    @Value("${ocr.vision.timeout-seconds:60}")
    private int visionTimeoutSeconds;

    @Value("${ocr.vision.prompt:You are extracting text from a Last War: Survival mobile game ranking screenshot. " +
            "Return only the raw text you see, line by line, exactly as it appears. " +
            "Include player names, alliance tags like [ANO], point values with commas, and rank numbers. " +
            "Do not add explanations, headers, or any extra formatting. Return plain text only.}")
    private String visionPrompt;

    @Bean
    public OcrStrategy ocrStrategy() {
        OcrStrategy strategy = switch (strategyName.toLowerCase()) {
            case "mock"       -> new MockOcrStrategy();
            case "vision-llm" -> new VisionLlmOcrStrategy(
                    visionBaseUrl, visionModel, visionApiKey, visionPrompt, visionTimeoutSeconds);
            default           -> new TesseractOcrStrategy(tessDataPath);
        };
        log.info("OCR strategy active: {}", strategy.name());
        return strategy;
    }
}