package com.lastwar.ano2193.ocr;

import java.io.File;

/**
 * OCR strategy that always returns a fixed hardcoded text regardless of the image.
 * Useful for local development and integration testing without a Tesseract installation.
 */
public class MockOcrStrategy implements OcrStrategy {

    static final String MOCK_TEXT =
            "RANKING\n" +
            "Daily Rank Weekly Rank\n" +
            "Mon. Tues. Wed. Thur. Fri. Sat.\n" +
            "Ranking Commander Points\n" +
            "— MockPlayer1\n" +
            "1 [ANO] A New Order 12,000,000\n" +
            "MockPlayer2\n" +
            "2 [ANO] A New Order 10,500,000\n" +
            "MockPlayer3\n" +
            "3 [ANO] A New Order 9,800,000\n" +
            "MockPlayer4\n" +
            "4 [ANO] A New Order 8,750,000\n" +
            "MockPlayer5\n" +
            "5 [ANO] A New Order 7,200,000\n";

    @Override
    public String extractText(File imageFile) {
        return MOCK_TEXT;
    }

    @Override
    public String name() {
        return "mock";
    }
}