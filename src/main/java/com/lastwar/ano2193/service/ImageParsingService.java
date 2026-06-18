package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.RankingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for parsing uploaded images and extracting ranking data.
 *
 * <p>Currently provides a stub implementation that returns no entries.
 * To enable OCR-based extraction, integrate Tesseract via the tess4j library:
 * <ol>
 *   <li>Add {@code net.sourceforge.tess4j:tess4j:5.11.0} to pom.xml</li>
 *   <li>Install Tesseract on the host (e.g. {@code apt install tesseract-ocr})</li>
 *   <li>Implement the body of {@link #parseImage} using {@code ITesseract}</li>
 * </ol>
 */
@Service
public class ImageParsingService {

    private static final Logger log = LoggerFactory.getLogger(ImageParsingService.class);

    /**
     * Parses an image file and extracts ranking entries.
     *
     * @param imageFile   the uploaded image
     * @param category    ranking category (e.g. "kills", "power")
     * @param submittedBy username of the uploader
     * @return list of extracted {@link RankingEntry} objects (empty until OCR is integrated)
     */
    public List<RankingEntry> parseImage(File imageFile, String category, String submittedBy) {
        log.info("Parsing image '{}' for category '{}'", imageFile.getName(), category);

        // ── OCR integration point ───────────────────────────────────────────────
        // ITesseract tesseract = new Tesseract();
        // tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
        // String text = tesseract.doOCR(imageFile);
        // return parseOcrText(text, category, submittedBy, imageFile.getName());
        // ────────────────────────────────────────────────────────────────────────

        log.warn("OCR not yet integrated – returning empty result for '{}'", imageFile.getName());
        return Collections.emptyList();
    }

    /**
     * Parses plain-text OCR output and attempts to build {@link RankingEntry} objects.
     * Each non-blank line is treated as one player row; numbers are extracted heuristically.
     *
     * @param ocrText     raw text from OCR engine
     * @param category    ranking category
     * @param submittedBy uploader username
     * @param sourcePhoto original filename stored for traceability
     * @return parsed entries (may be empty or partially filled)
     */
    public List<RankingEntry> parseOcrText(String ocrText, String category,
                                    String submittedBy, String sourcePhoto) {
        log.debug("parseOcrText: category={} submittedBy={} sourcePhoto={}", category, submittedBy, sourcePhoto);
        List<RankingEntry> entries = new ArrayList<>();
        if (ocrText == null || ocrText.isBlank()) {
            log.debug("parseOcrText: input is blank, returning empty list");
            return entries;
        }

        String[] lines = ocrText.split("\\r?\\n");
        log.trace("parseOcrText: rawLineCount={}", lines.length);
        int autoRank = 1;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            log.trace("parseOcrText: processing line rank={} raw='{}'", autoRank, line);
            RankingEntry entry = new RankingEntry();
            entry.setRank(autoRank++);
            entry.setCategory(category);
            entry.setSubmittedBy(submittedBy);
            entry.setSourcePhotoPath(sourcePhoto);
            entry.setCapturedAt(LocalDateTime.now());

            // Heuristic: first token that looks like a word is the player name,
            // subsequent numeric tokens are power / kills respectively.
            String[] tokens = line.split("\\s+");
            boolean nameCaptured = false;
            int numericIndex = 0;
            for (String token : tokens) {
                String digits = token.replaceAll("[^0-9]", "");
                if (!digits.isEmpty() && digits.length() >= 4) {
                    long value = Long.parseLong(digits);
                    if (numericIndex == 0) {
                        log.trace("parseOcrText: token='{}' → power={}", token, value);
                        entry.setPower(value);
                    } else if (numericIndex == 1) {
                        log.trace("parseOcrText: token='{}' → kills={}", token, value);
                        entry.setKills(value);
                    }
                    numericIndex++;
                } else if (!nameCaptured && token.matches("[A-Za-z].*")) {
                    log.trace("parseOcrText: token='{}' → playerName", token);
                    entry.setPlayerName(token);
                    nameCaptured = true;
                }
            }
            log.trace("parseOcrText: entry rank={} playerName={} power={} kills={}",
                    entry.getRank(), entry.getPlayerName(), entry.getPower(), entry.getKills());
            entries.add(entry);
        }
        log.debug("parseOcrText: parsed {} entries", entries.size());
        return entries;
    }
}
