package com.lastwar.ano2193;

import com.lastwar.ano2193.service.ImageParsingService;
import com.lastwar.ano2193.model.RankingEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LastWarApplicationTests {

    @Autowired
    private ImageParsingService imageParsingService;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
    }

    @Test
    void parseOcrText_emptyInput_returnsEmptyList() {
        List<RankingEntry> result = imageParsingService.parseOcrText("", "kills", "tester", "test.png");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseOcrText_singleEntry_extractsNameTagAndPoints() {
        String ocrText = "PlayerOne\n[ANO] A New Order 12345678";
        List<RankingEntry> result = imageParsingService.parseOcrText(ocrText, "power", "tester", "photo.png");

        assertFalse(result.isEmpty());
        RankingEntry entry = result.get(0);
        assertEquals(1, entry.getRank());
        assertEquals("PlayerOne", entry.getPlayerName());
        assertEquals("ANO", entry.getAllianceTag());
        assertEquals(12345678L, entry.getPower());
        assertEquals("power", entry.getCategory());
        assertEquals("tester", entry.getSubmittedBy());
        assertEquals("photo.png", entry.getSourcePhotoPath());
        assertNotNull(entry.getCapturedAt());
    }

    @Test
    void parseOcrText_multipleEntries_rankSequentially() {
        String ocrText = """
                PHiLL
                [ANO] A New Order 34802316
                final day
                [BEZT] BADAZZEZ 32284398
                SkaterBoi
                [ANO] A New Order 29882900
                """;
        List<RankingEntry> result = imageParsingService.parseOcrText(ocrText, "points", "tester", "photo.png");

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getRank());
        assertEquals("PHiLL", result.get(0).getPlayerName());
        assertEquals("ANO", result.get(0).getAllianceTag());
        assertEquals(34802316L, result.get(0).getPower());

        assertEquals(2, result.get(1).getRank());
        assertEquals("final day", result.get(1).getPlayerName());
        assertEquals("BEZT", result.get(1).getAllianceTag());

        assertEquals(3, result.get(2).getRank());
        assertEquals("SkaterBoi", result.get(2).getPlayerName());
    }

    @Test
    void parseOcrText_nameAndTagOnSameLine_extracted() {
        // OCR sometimes merges both parts onto one line
        String ocrText = "2Hann [ANO] A New Order 29297918";
        List<RankingEntry> result = imageParsingService.parseOcrText(ocrText, "points", "tester", "photo.png");

        assertEquals(1, result.size());
        assertEquals("2Hann", result.get(0).getPlayerName());
        assertEquals("ANO", result.get(0).getAllianceTag());
        assertEquals(29297918L, result.get(0).getPower());
    }

    @Test
    void parseOcrText_realScreenshotOcr_extractsFiveEntries() {
        // Exact OCR output produced by Tesseract 5.3.4 on ranking-sample.png.
        // Rank 26 (javalinho) is absent from the result because OCR merged "]A"
        // into "JA", yielding "[ANOJA" which has no closing bracket and is skipped.
        String ocrText = """
                Mon. Tues. Wed. Thur. Fri. Sat.
                Ranking Commander Points
                P23 (Gen) PHILL
                yy Esc is 34,802,316
                — Ln [ANO] A New Order
                Sy f final d.
                Tez "— 32,284,398
                ts [BEZT] BADAZZEZ
                L—J WEN) SkaterBoi
                ey (ie 29,882,900
                a4) [ANO] A New Order
                Sag) 2Hann
                (4) = 29,297,918
                =) [ANO] A New Order
                Multihunter
                B a 27,970,364
                idéinex [ANO] A New Order
                Sa ee _ ee ae oe
                mee javalinho
                26 | J 14,595,408
                ex L[ANOJA New Order
                """;

        List<RankingEntry> result = imageParsingService.parseOcrText(
                ocrText, "points", "tester", "ranking-sample.png");

        assertEquals(5, result.size());

        RankingEntry r1 = result.get(0);
        assertEquals(1, r1.getRank());
        assertTrue(r1.getPlayerName().toLowerCase().contains("phill"));
        assertEquals("ANO", r1.getAllianceTag());
        assertEquals(34_802_316L, r1.getPower());

        RankingEntry r2 = result.get(1);
        assertEquals(2, r2.getRank());
        assertEquals("BEZT", r2.getAllianceTag());
        assertEquals(32_284_398L, r2.getPower());

        RankingEntry r3 = result.get(2);
        assertEquals(3, r3.getRank());
        assertTrue(r3.getPlayerName().contains("SkaterBoi"));
        assertEquals("ANO", r3.getAllianceTag());
        assertEquals(29_882_900L, r3.getPower());

        RankingEntry r4 = result.get(3);
        assertEquals(4, r4.getRank());
        assertTrue(r4.getPlayerName().contains("2Hann"));
        assertEquals("ANO", r4.getAllianceTag());
        assertEquals(29_297_918L, r4.getPower());

        RankingEntry r5 = result.get(4);
        assertEquals(5, r5.getRank());
        assertEquals("Multihunter", r5.getPlayerName());
        assertEquals("ANO", r5.getAllianceTag());
        assertEquals(27_970_364L, r5.getPower());
    }

    @Test
    void parseOcrText_headerLinesSkipped() {
        // UI chrome lines (day tabs, column headers) must not produce entries
        String ocrText = """
                Mon.
                Ranking
                Commander
                Points
                PHiLL
                [ANO] A New Order 34802316
                """;
        List<RankingEntry> result = imageParsingService.parseOcrText(ocrText, "points", "tester", "photo.png");

        assertEquals(1, result.size());
        assertEquals("PHiLL", result.get(0).getPlayerName());
    }
}
