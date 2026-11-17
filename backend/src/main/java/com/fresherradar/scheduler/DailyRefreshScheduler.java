package com.fresherradar.scheduler;

import com.fresherradar.service.CollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "fresherradar.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DailyRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyRefreshScheduler.class);

    private final CollectionService collection;

    public DailyRefreshScheduler(CollectionService collection) {
        this.collection = collection;
    }

    @Scheduled(cron = "${fresherradar.scheduler.cron}", zone = "${fresherradar.scheduler.zone}")
    public void refresh() {
        log.info("Scheduled daily refresh starting");
        try {
            CollectionService.RefreshSummary s = collection.refreshAll();
            log.info("Scheduled refresh: {} (fetched={}, inserted={}, duplicates={})",
                    s.message(), s.fetched(), s.inserted(), s.duplicates());
        } catch (Exception e) {
            log.error("Scheduled refresh failed", e);
        }
    }
}
