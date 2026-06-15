package com.lastwar.ano2193.scheduler;

import com.lastwar.ano2193.service.CsvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically exports in-memory ranking data to CSV.
 * The interval is configured via {@code app.csv.persist.interval} (milliseconds).
 * Default: 300 000 ms (5 minutes).
 */
@Component
public class CsvPersistenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(CsvPersistenceScheduler.class);

    private final CsvService csvService;

    public CsvPersistenceScheduler(CsvService csvService) {
        this.csvService = csvService;
    }

    @Scheduled(fixedRateString = "${app.csv.persist.interval:300000}")
    public void persistToCsv() {
        log.debug("Scheduled CSV persistence triggered");
        csvService.exportRankingsToCsv();
    }
}
