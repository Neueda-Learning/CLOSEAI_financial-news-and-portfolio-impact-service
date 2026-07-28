package com.fnpis.api.internal;

import com.fnpis.scheduler.NewsFetchScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NewsFetchController {

    private final NewsFetchScheduler scheduler;

    public NewsFetchController(NewsFetchScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/news/fetch")
    public ResponseEntity<Void> triggerFetch() {
        scheduler.manualFetch();
        return ResponseEntity.accepted().build();
    }
}
