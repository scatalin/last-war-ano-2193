package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.ocr.OcrException;
import com.lastwar.ano2193.ocr.OcrStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageParsingService {

    private static final Logger log = LoggerFactory.getLogger(ImageParsingService.class);

    // Matches alliance tags like [ANO] or [BEZT]
    private static final Pattern ALLIANCE_TAG  = Pattern.compile("\\[([A-Za-z0-9]+)\\]");
    // Matches digit+comma sequences — used to locate point values
    private static final Pattern NUMBER_LIKE   = Pattern.compile("[\\d,]+");
    // Strips a leading rank number ("4 ", "26 ") from a line
    private static final Pattern LEADING_RANK  = Pattern.compile("^(\\d{1,3})\\s+");
    // UI chrome lines we want to skip (day tabs, column headers, bare numbers)
    private static final Pattern HEADER_LINE   = Pattern.compile(
        "(?i)^(rank(?:ing)?|commander|points?|mon\\.?|tue?s?\\.?|wed\\.?|thu?r?\\.?|fri\\.?|sat\\.?|sun\\.?|\\d+)$");

    private final OcrStrategy ocrStrategy;

    public ImageParsingService(OcrStrategy ocrStrategy) {
        this.ocrStrategy = ocrStrategy;
    }

    /**
     * Runs the configured OCR strategy on {@code imageFile} and returns the raw text.
     *
     * @throws OcrException if the underlying engine fails
     */
    public String extractRawText(File imageFile) throws OcrException {
        log.info("OCR start: strategy={} file='{}' size={}B",
                ocrStrategy.name(), imageFile.getName(), imageFile.length());
        String result = ocrStrategy.extractText(imageFile);
        boolean blank = result == null || result.isBlank();
        log.info("OCR end: file='{}' chars={} blank={}",
                imageFile.getName(), result == null ? -1 : result.length(), blank);
        if (blank) {
            log.warn("OCR returned blank text for '{}' — image may be unreadable or unsupported",
                    imageFile.getName());
        } else {
            log.info("OCR raw text for '{}':\n>>>\n{}\n<<<", imageFile.getName(), result);
        }
        return result;
    }

    public List<RankingEntry> parseImage(File imageFile, String category, String submittedBy) {
        log.info("Parsing image '{}' for category '{}'", imageFile.getName(), category);
        try {
            String text = extractRawText(imageFile);
            log.debug("OCR raw output for '{}':\n{}", imageFile.getName(), text);
            return parseOcrText(text, category, submittedBy, imageFile.getName());
        } catch (OcrException e) {
            log.error("OCR failed for '{}': {}", imageFile.getName(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses OCR text from a Last War commander-ranking screenshot.
     *
     * Each leaderboard entry produces two OCR lines:
     *   Line 1 — player name  (e.g. "PHiLL" or "final day")
     *   Line 2 — alliance tag + alliance name + points  (e.g. "[ANO] A New Order  34,802,316")
     *
     * A line containing [TAG] triggers entry creation; the preceding non-header
     * line is used as the player name.  When both pieces are on a single line
     * (player name before [TAG]) that is handled too.
     *
     * The rightmost large number (≥ 5 digits) on the alliance line is stored as
     * {@code power} — the primary ranking metric for this screenshot type.
     */
    public List<RankingEntry> parseOcrText(String ocrText, String category,
                                           String submittedBy, String sourcePhoto) {
        log.debug("parseOcrText: category={} submittedBy={} sourcePhoto={}", category, submittedBy, sourcePhoto);
        List<RankingEntry> entries = new ArrayList<>();
        if (ocrText == null || ocrText.isBlank()) {
            log.debug("parseOcrText: input is blank, returning empty list");
            return entries;
        }

        String[] rawLines = ocrText.split("\\r?\\n");
        log.trace("parseOcrText: rawLineCount={}", rawLines.length);

        int autoRank = 1;
        String pendingPlayerName = null;
        Long pendingPoints = null;

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty() || HEADER_LINE.matcher(line).matches()) continue;

            log.trace("parseOcrText: line='{}'", line);
            Matcher tagMatcher = ALLIANCE_TAG.matcher(line);

            if (tagMatcher.find()) {
                String allianceTag = tagMatcher.group(1);

                // Player name may be embedded before [TAG] on the same line
                String beforeTag = line.substring(0, tagMatcher.start()).trim();
                Matcher leadRank = LEADING_RANK.matcher(beforeTag);
                if (leadRank.find()) beforeTag = beforeTag.substring(leadRank.end()).trim();
                if (!beforeTag.isEmpty() && pendingPlayerName == null) {
                    pendingPlayerName = beforeTag;
                }

                // Points are on the preceding non-tag line (pendingPoints); fall back to
                // extracting from this line for simpler single-line input formats.
                Long points = pendingPoints != null ? pendingPoints : extractLastLargeNumber(line);

                RankingEntry entry = new RankingEntry();
                entry.setRank(autoRank++);
                entry.setCategory(category);
                entry.setSubmittedBy(submittedBy);
                entry.setSourcePhotoPath(sourcePhoto);
                entry.setCapturedAt(LocalDateTime.now());
                entry.setPlayerName(pendingPlayerName);
                entry.setAllianceTag(allianceTag);
                if (points != null) entry.setPower(points);

                log.trace("parseOcrText: entry rank={} playerName={} tag={} power={}",
                        entry.getRank(), entry.getPlayerName(), entry.getAllianceTag(), entry.getPower());
                entries.add(entry);
                pendingPlayerName = null;
                pendingPoints = null;

            } else {
                // Non-tag line: either a player-name line or a points-bearing line.
                // In game screenshots the rightmost column (points) appears on a separate
                // OCR line from the alliance tag, so we split the two concerns:
                //   - line with a large number  → capture as pendingPoints, keep existing name
                //   - line without a large number → treat as player name
                String clean = line;
                Matcher leadRank = LEADING_RANK.matcher(clean);
                if (leadRank.find()) clean = clean.substring(leadRank.end()).trim();

                if (!clean.isEmpty() && !clean.matches("\\d+")) {
                    Long linePoints = extractLastLargeNumber(clean);
                    if (linePoints != null) {
                        pendingPoints = linePoints;
                    } else {
                        pendingPlayerName = clean;
                    }
                    log.trace("parseOcrText: pending playerName='{}' pendingPoints={}",
                            pendingPlayerName, pendingPoints);
                }
            }
        }

        log.debug("parseOcrText: parsed {} entries", entries.size());
        return entries;
    }

    /** Returns the rightmost number with ≥ 5 digits (≥ 10 000) found in {@code line}. */
    private Long extractLastLargeNumber(String line) {
        Matcher m = NUMBER_LIKE.matcher(line);
        Long result = null;
        while (m.find()) {
            String raw = m.group().replaceAll(",", "");
            if (raw.length() >= 5) {
                try { result = Long.parseLong(raw); } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}