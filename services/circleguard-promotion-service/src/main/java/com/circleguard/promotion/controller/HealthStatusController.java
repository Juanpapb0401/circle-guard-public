package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.HealthStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthStatusController {
    private final HealthStatusService statusService;

    @PostMapping("/report")
    public ResponseEntity<Void> reportStatus(@RequestBody Map<String, String> request) {
        String anonymousId = request.get("anonymousId");
        String status = request.get("status");
        
        statusService.updateStatus(anonymousId, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recovery/{id}")
    public ResponseEntity<Void> recover(@PathVariable String id) {
        statusService.promoteToRecovered(id);
        return ResponseEntity.ok().build();
    }
}
