package com.circleguard.auth.controller;

import com.circleguard.auth.service.JwtTokenService;
import com.circleguard.auth.client.IdentityClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authManager;
    private final JwtTokenService jwtService;
    private final IdentityClient identityClient;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // 1. Authenticate (Dual-Chain)
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // 2. Anonymize (Fetch/Create Anonymous ID from Identity Service)
        // For PoC, we assume the user's 'realIdentity' is their email/username
        UUID anonymousId = identityClient.getAnonymousId(username);

        // 3. Issue Token
        String token = jwtService.generateToken(anonymousId, auth);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "anonymousId", anonymousId.toString()
        ));
    }
}
