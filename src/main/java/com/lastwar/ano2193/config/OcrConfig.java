package com.lastwar.ano2193.config;

import com.lastwar.ano2193.ocr.MockOcrStrategy;
import com.lastwar.ano2193.ocr.OcrStrategy;
import com.lastwar.ano2193.ocr.TesseractOcrStrategy;
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

    @Value("${ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}")
    private String tessDataPath;

    @Bean
    public OcrStrategy ocrStrategy() {
        OcrStrategy strategy = "mock".equalsIgnoreCase(strategyName)
                ? new MockOcrStrategy()
                : new TesseractOcrStrategy(tessDataPath);
        log.info("OCR strategy: {} (tessdata-path='{}')", strategy.name(), tessDataPath);
        return strategy;
    }
}