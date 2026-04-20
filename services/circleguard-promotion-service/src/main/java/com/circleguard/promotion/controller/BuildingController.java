package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.BuildingService;
import com.circleguard.promotion.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;
    private final FloorService floorService;

    @PostMapping
    public ResponseEntity<Building> createBuilding(@RequestBody Map<String, String> request) {
        Building building = buildingService.createBuilding(
                request.get("name"),
                request.get("code"),
                request.get("description")
        );
        return ResponseEntity.ok(building);
    }

    @GetMapping
    public ResponseEntity<List<Building>> listBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

    @GetMapping("/{id}/floors")
    public ResponseEntity<List<Floor>> getFloors(@PathVariable UUID id) {
        return ResponseEntity.ok(floorService.getFloorsByBuilding(id));
    }

    @PostMapping("/{id}/floors")
    public ResponseEntity<Floor> addFloor(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        Floor floor = floorService.addFloor(
                id,
                (Integer) request.get("floorNumber"),
                (String) request.get("name")
        );
        return ResponseEntity.ok(floor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        Building building = buildingService.updateBuilding(
                id,
                request.get("name"),
                request.get("code"),
                request.get("description")
        );
        return ResponseEntity.ok(building);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.ok().build();
    }
}
