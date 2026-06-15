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
    void parseOcrText_singleLine_createsOneEntry() {
        String ocrText = "PlayerOne 12345678 9876543";
        List<RankingEntry> result = imageParsingService.parseOcrText(ocrText, "power", "tester", "photo.png");

        assertFalse(result.isEmpty());
        RankingEntry entry = result.get(0);
        assertEquals(1, entry.getRank());
        assertEquals("power", entry.getCategory());
        assertEquals("tester", entry.getSubmittedBy());
        assertEquals("photo.png", entry.getSourcePhotoPath());
        assertNotNull(entry.getCapturedAt());
    }
}
