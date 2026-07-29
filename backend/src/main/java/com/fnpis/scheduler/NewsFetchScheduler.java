package com.fnpis.scheduler;

import com.fnpis.common.error.ApiException;
import com.fnpis.service.NewsFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchScheduler.class);
    private final NewsFetchService service;

    public NewsFetchScheduler(NewsFetchService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.schedule.news-poll-delay}")
    public void scheduledFetch() {
        if (!service.tryAcquire()) {
            log.debug("News fetch skipped — already running");
            return;
        }
        try {
            service.fetchAll();
        } finally {
            service.release();
        }
    }

    /** Manual trigger via HTTP. Returns 409 if a fetch is already running. */
    public void manualFetch() {
        if (!service.tryAcquire()) {
            throw ApiException.taskAlreadyRunning("news-fetch");
        }
        try {
            service.fetchAll();
        } finally {
            service.release();
        }
    }
}
