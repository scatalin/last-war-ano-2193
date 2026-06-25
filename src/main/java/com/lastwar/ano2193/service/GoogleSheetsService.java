package com.lastwar.ano2193.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.lastwar.ano2193.config.GoogleSheetsConfig;
import com.lastwar.ano2193.model.RankingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final String APP_NAME = "LastWar-ANO2193";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GoogleSheetsConfig config;

    public GoogleSheetsService(GoogleSheetsConfig config) {
        this.config = config;
    }

    public String testConnection() throws IOException, GeneralSecurityException {
        log.debug("testConnection spreadsheetId={}", config.getSpreadsheetId());
        Sheets sheets = buildClient();
        Spreadsheet spreadsheet = sheets.spreadsheets()
                .get(config.getSpreadsheetId())
                .execute();
        String title = spreadsheet.getProperties().getTitle();
        log.debug("testConnection OK title={}", title);
        return title;
    }

    public int exportRankings(List<RankingEntry> entries) throws IOException, GeneralSecurityException {
        log.debug("exportRankings count={}", entries.size());
        Sheets sheets = buildClient();
        String spreadsheetId = config.getSpreadsheetId();
        String sheetName = config.getSheetName();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("Rank", "Player", "Alliance", "Power", "Kills",
                "Category", "Event Tag", "Submitted By", "Captured At"));
        for (RankingEntry e : entries) {
            rows.add(Arrays.asList(
                    e.getRank() != null ? e.getRank() : "",
                    e.getPlayerName() != null ? e.getPlayerName() : "",
                    e.getAllianceTag() != null ? e.getAllianceTag() : "",
                    e.getPower() != null ? e.getPower() : "",
                    e.getKills() != null ? e.getKills() : "",
                    e.getCategory() != null ? e.getCategory() : "",
                    e.getEventTag() != null ? e.getEventTag() : "",
                    e.getSubmittedBy() != null ? e.getSubmittedBy() : "",
                    e.getCapturedAt() != null ? e.getCapturedAt().format(DT_FMT) : ""
            ));
        }

        sheets.spreadsheets().values()
                .clear(spreadsheetId, sheetName, new ClearValuesRequest())
                .execute();

        ValueRange body = new ValueRange().setValues(rows);
        sheets.spreadsheets().values()
                .update(spreadsheetId, sheetName + "!A1", body)
                .setValueInputOption("RAW")
                .execute();

        log.debug("exportRankings complete: {} data rows written", entries.size());
        return entries.size();
    }

    private Sheets buildClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials;
        try (FileInputStream is = new FileInputStream(config.getCredentialsFile())) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(SheetsScopes.SPREADSHEETS);
        }
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APP_NAME)
                .build();
    }
}