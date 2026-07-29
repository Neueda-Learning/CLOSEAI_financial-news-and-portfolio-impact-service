package com.fnpis.api.internal;

import com.fnpis.scheduler.QuoteRefreshScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QuoteRefreshController {

    private final QuoteRefreshScheduler scheduler;

    public QuoteRefreshController(QuoteRefreshScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/quotes/refresh")
    public ResponseEntity<Void> triggerRefresh() {
        scheduler.manualRefresh();
        return ResponseEntity.accepted().build();
    }
}
