package com.lastwar.ano2193;

import com.lastwar.ano2193.model.RankingEntry;
import com.lastwar.ano2193.ocr.OcrException;
import com.lastwar.ano2193.ocr.OcrStrategy;
import com.lastwar.ano2193.service.ImageParsingService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImageParsingService.parseOcrText().
 * No Spring context, no OCR engine — drives the parser directly with hardcoded text.
 *
 * Covers the two formats a vision-LLM or Tesseract is likely to produce:
 *   - two-line: player name on one line, alliance tag + points on the next
 *   - inline:   rank number + player + alliance tag + points all on one line
 */
class ImageParsingServiceParseOcrTextTest {

    private static final OcrStrategy NO_OP = new OcrStrategy() {
        @Override public String extractText(File f) throws OcrException {
            throw new OcrException("should not be called", null);
        }
        @Override public String name() { return "test-noop"; }
    };

    private final ImageParsingService svc = new ImageParsingService(NO_OP);

    /**
     * Simulates a clean vision-LLM response where each entry spans two lines:
     *   player name
     *   [ALLIANCE] Alliance Name  points
     *
     * This is the format the prompt encourages LLaVA to produce.
     */
    @Test
    void parseOcrText_twoLineFormat_extractsAllEntries() {
        String text = """
                buubeats
                [ANO] A New Order 10,181,750
                Jsu08
                [ANO] A New Order 9,557,250
                WhitE WolF 13
                [ANO] A New Order 9,287,375
                """;

        List<RankingEntry> entries = svc.parseOcrText(text, "points", "test-user", "test.png");

        assertEquals(3, entries.size());

        assertEntry(entries.get(0), 1, "buubeats",      "ANO", 10_181_750L);
        assertEntry(entries.get(1), 2, "Jsu08",         "ANO",  9_557_250L);
        assertEntry(entries.get(2), 3, "WhitE WolF 13", "ANO",  9_287_375L);
    }

    /**
     * gpt-4o returns each entry as four separate lines:
     *   rank number, player name, alliance line, points
     *
     * Verifies both that points go to the correct entry (not shifted to the next)
     * and that the rank number from the image is used rather than a sequential counter.
     */
    @Test
    void parseOcrText_fourLineGptFormat_mapsRankAndPoints() {
        String text = """
                1
                PHiLL
                [ANO] A New Order
                34,802,316
                2
                final day
                [BEZT] BADAZZZEZ
                32,284,398
                """;

        List<RankingEntry> entries = svc.parseOcrText(text, "points", "test-user", "test.png");

        assertEquals(2, entries.size());
        assertEntry(entries.get(0), 1, "PHiLL",     "ANO",  34_802_316L);
        assertEntry(entries.get(1), 2, "final day", "BEZT", 32_284_398L);
    }

    /**
     * Non-sequential rank (e.g. "Your Alliance" row at rank 23) must use the
     * image rank, not a sequential counter that would give rank 3 here.
     */
    @Test
    void parseOcrText_nonSequentialRank_usesImageRank() {
        String text = """
                1
                buubeats
                [ANO] A New Order
                10,181,750
                2
                Jsu08
                [ANO] A New Order
                9,557,250
                23
                javalinho
                [ANO] A New Order
                8,139,500
                """;

        List<RankingEntry> entries = svc.parseOcrText(text, "points", "test-user", "test.png");

        assertEquals(3, entries.size());
        assertEntry(entries.get(0),  1, "buubeats",  "ANO", 10_181_750L);
        assertEntry(entries.get(1),  2, "Jsu08",     "ANO",  9_557_250L);
        assertEntry(entries.get(2), 23, "javalinho", "ANO",  8_139_500L);
    }

    /**
     * Simulates a compact vision-LLM response where each entry is a single line
     * prefixed with a rank number — a common LLaVA output style.
     *
     * The parser should strip the leading rank digit and still extract the player name.
     */
    @Test
    void parseOcrText_inlineWithRankPrefix_extractsAllEntries() {
        String text = """
                1 buubeats [ANO] A New Order 10,181,750
                2 Jsu08 [ANO] A New Order 9,557,250
                3 WhitE WolF 13 [ANO] A New Order 9,287,375
                """;

        List<RankingEntry> entries = svc.parseOcrText(text, "points", "test-user", "test.png");

        assertEquals(3, entries.size());

        assertEntry(entries.get(0), 1, "buubeats",      "ANO", 10_181_750L);
        assertEntry(entries.get(1), 2, "Jsu08",         "ANO",  9_557_250L);
        assertEntry(entries.get(2), 3, "WhitE WolF 13", "ANO",  9_287_375L);
    }

    /**
     * Simulates LLaVA output that includes the "Your Alliance" footer line and
     * noisy header lines (which the real screenshot contains).  Those should be
     * ignored so the entry count and ranks are not thrown off.
     */
    @Test
    void parseOcrText_skipsHeaderAndFooterNoise() {
        String text = """
                RANKING
                Daily Rank Weekly Rank
                Mon. Tues. Wed. Thur. Fri. Sat.
                Ranking Commander Points
                buubeats
                [ANO] A New Order 10,181,750
                Jsu08
                [ANO] A New Order 9,557,250
                Your Alliance
                """;

        List<RankingEntry> entries = svc.parseOcrText(text, "points", "test-user", "test.png");

        assertEquals(2, entries.size());
        assertEntry(entries.get(0), 1, "buubeats", "ANO", 10_181_750L);
        assertEntry(entries.get(1), 2, "Jsu08",    "ANO",  9_557_250L);
    }

    /**
     * Verifies mandatory fields are populated even when the text is minimal.
     */
    @Test
    void parseOcrText_setsMetadataFields() {
        String text = "Tester\n[ANO] A New Order 5,000,000\n";

        List<RankingEntry> entries = svc.parseOcrText(text, "power", "alice", "photo.png");

        assertEquals(1, entries.size());
        RankingEntry e = entries.get(0);
        assertEquals("power",     e.getCategory());
        assertEquals("alice",     e.getSubmittedBy());
        assertEquals("photo.png", e.getSourcePhotoPath());
        assertNotNull(e.getCapturedAt());
    }

    /**
     * Empty or blank input must return an empty list without throwing.
     */
    @Test
    void parseOcrText_blankInput_returnsEmpty() {
        assertTrue(svc.parseOcrText(null,  "points", "u", "f").isEmpty());
        assertTrue(svc.parseOcrText("",    "points", "u", "f").isEmpty());
        assertTrue(svc.parseOcrText("   ", "points", "u", "f").isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void assertEntry(RankingEntry e, int rank, String player, String tag, long power) {
        assertEquals(rank,   e.getRank(),        "rank");
        assertEquals(player, e.getPlayerName(),  "playerName");
        assertEquals(tag,    e.getAllianceTag(),  "allianceTag");
        assertEquals(power,  e.getPower(),        "power");
    }
}