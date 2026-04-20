package com.circleguard.identity.controller;

import com.circleguard.identity.service.IdentityVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/identities")
@RequiredArgsConstructor
public class IdentityVaultController {
    private final IdentityVaultService vaultService;

    /**
     * Maps a real identity to an anonymous ID. 
     * Usually called during onboarding/auth.
     */
    @PostMapping("/map")
    public ResponseEntity<Map<String, UUID>> mapIdentity(@RequestBody Map<String, String> request) {
        String realIdentity = request.get("realIdentity");
        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);
        return ResponseEntity.ok(Map.of("anonymousId", anonymousId));
    }

    /**
     * Restricted lookup of real identity from anonymous ID.
     * Authorized for Health Center personnel only.
     */
    @GetMapping("/lookup/{id}")
    @PreAuthorize("hasAuthority('identity:lookup')")
    public ResponseEntity<Map<String, String>> lookupIdentity(@PathVariable UUID id) {
        String realIdentity = vaultService.resolveRealIdentity(id);
        // TODO: Emit Audit Event to Kafka
        return ResponseEntity.ok(Map.of("realIdentity", realIdentity));
    }
}
