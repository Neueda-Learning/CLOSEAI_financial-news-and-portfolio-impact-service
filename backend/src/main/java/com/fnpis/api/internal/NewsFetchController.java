package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.NewsRefreshResponse;
import com.fnpis.scheduler.NewsFetchScheduler;
import com.fnpis.service.NewsFetchService.FetchResult;
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

    @PostMapping("/news/refresh")
    public ResponseEntity<NewsRefreshResponse> triggerRefresh() {
        FetchResult r = scheduler.manualFetch();
        return ResponseEntity.ok(new NewsRefreshResponse(r.inserted(), r.skippedDuplicates()));
    }
}
