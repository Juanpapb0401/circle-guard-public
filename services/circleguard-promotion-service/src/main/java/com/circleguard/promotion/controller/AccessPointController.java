package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.service.AccessPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access-points")
@RequiredArgsConstructor
public class AccessPointController {
    private final AccessPointService accessPointService;

    @GetMapping("/{id}")
    public ResponseEntity<AccessPoint> getAccessPoint(@PathVariable UUID id) {
        return accessPointService.getAccessPoint(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccessPoint> updateAccessPoint(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        AccessPoint ap = accessPointService.updateAccessPoint(
                id,
                (String) request.get("macAddress"),
                Double.valueOf(request.get("coordinateX").toString()),
                Double.valueOf(request.get("coordinateY").toString()),
                (String) request.get("name")
        );
        return ResponseEntity.ok(ap);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccessPoint(@PathVariable UUID id) {
        accessPointService.deleteAccessPoint(id);
        return ResponseEntity.ok().build();
    }
}
