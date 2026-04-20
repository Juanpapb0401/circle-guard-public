import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.AccessPointService;
import com.circleguard.promotion.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/floors")
@RequiredArgsConstructor
public class FloorController {
    private final FloorService floorService;
    private final AccessPointService accessPointService;

    @PostMapping("/{id}/access-points")
    public ResponseEntity<AccessPoint> addAccessPoint(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        AccessPoint ap = accessPointService.registerAccessPoint(
                id,
                (String) request.get("macAddress"),
                Double.valueOf(request.get("coordinateX").toString()),
                Double.valueOf(request.get("coordinateY").toString()),
                (String) request.get("name")
        );
        return ResponseEntity.ok(ap);
    }

    @GetMapping("/{id}/access-points")
    public ResponseEntity<List<AccessPoint>> getAccessPoints(@PathVariable UUID id) {
        return ResponseEntity.ok(accessPointService.getAccessPointsByFloor(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Floor> updateFloor(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        Floor floor = floorService.updateFloor(
                id,
                (Integer) request.get("floorNumber"),
                (String) request.get("name")
        );
        return ResponseEntity.ok(floor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFloor(@PathVariable UUID id) {
        floorService.deleteFloor(id);
        return ResponseEntity.ok().build();
    }
}
