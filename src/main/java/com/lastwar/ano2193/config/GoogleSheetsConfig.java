package com.lastwar.ano2193.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.sheets")
public class GoogleSheetsConfig {

    private boolean enabled = false;
    private String credentialsFile = "";
    private String spreadsheetId = "";
    private String sheetName = "Rankings";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCredentialsFile() { return credentialsFile; }
    public void setCredentialsFile(String credentialsFile) { this.credentialsFile = credentialsFile; }

    public String getSpreadsheetId() { return spreadsheetId; }
    public void setSpreadsheetId(String spreadsheetId) { this.spreadsheetId = spreadsheetId; }

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public boolean isConfigured() {
        return enabled
                && credentialsFile != null && !credentialsFile.isBlank()
                && spreadsheetId != null && !spreadsheetId.isBlank();
    }
}