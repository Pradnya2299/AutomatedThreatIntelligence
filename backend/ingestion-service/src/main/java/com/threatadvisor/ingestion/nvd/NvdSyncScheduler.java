package com.threatadvisor.ingestion.nvd;

import com.threatadvisor.ingestion.domain.NvdSyncState;
import com.threatadvisor.ingestion.service.NvdIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nvd.sync-enabled", havingValue = "true")
public class NvdSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(NvdSyncScheduler.class);

    private final NvdIngestionService nvdIngestionService;

    public NvdSyncScheduler(NvdIngestionService nvdIngestionService) {
        this.nvdIngestionService = nvdIngestionService;
    }

    @Scheduled(fixedDelayString = "${nvd.sync-interval-ms:3600000}", initialDelayString = "${nvd.sync-interval-ms:3600000}")
    public void sync() {
        log.info("operation=nvd.sync.start");
        NvdSyncState state = nvdIngestionService.synchronizeIncremental();
        log.info("operation=nvd.sync.finish status={}", state.getLastStatus());
    }
}
