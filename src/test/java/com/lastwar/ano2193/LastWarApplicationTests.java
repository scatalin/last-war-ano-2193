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
