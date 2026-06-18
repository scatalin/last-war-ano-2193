package com.lastwar.ano2193.ocr;

import java.io.File;

public interface OcrStrategy {
    /**
     * Extracts raw text from {@code imageFile}.
     *
     * @throws OcrException if the underlying engine fails
     */
    String extractText(File imageFile) throws OcrException;

    /** Human-readable name shown in logs and admin UI. */
    String name();
}