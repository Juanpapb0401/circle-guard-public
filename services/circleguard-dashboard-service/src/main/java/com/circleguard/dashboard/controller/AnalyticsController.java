package com.circleguard.dashboard.controller;

import com.circleguard.dashboard.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/trends/{locationId}")
    public ResponseEntity<List<Map<String, Object>>> getTrends(@PathVariable UUID locationId) {
        return ResponseEntity.ok(analyticsService.getEntryTrends(locationId));
    }

    @GetMapping("/health-board")
    public ResponseEntity<Map<String, Object>> getHealthBoardStats() {
        return ResponseEntity.ok(analyticsService.getGlobalHealthStats());
    }
}
