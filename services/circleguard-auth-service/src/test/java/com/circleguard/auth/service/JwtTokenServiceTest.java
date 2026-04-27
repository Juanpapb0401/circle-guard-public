package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtTokenServiceTest {

    private static final String SECRET = "my-super-secret-dev-key-32-chars-long-12345678";
    private static final long EXPIRATION = 3_600_000L;

    private JwtTokenService service;
    private Key verificationKey;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(SECRET, EXPIRATION);
        verificationKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    @Test
    void generateToken_subjectIsAnonymousId() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuth(List.of("ROLE_USER"));

        String token = service.generateToken(anonymousId, auth);

        assertThat(parseClaims(token).getSubject()).isEqualTo(anonymousId.toString());
    }

    @Test
    void generateToken_containsPermissionsClaim() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mockAuth(List.of("ROLE_USER", "SURVEY_SUBMIT"));

        String token = service.generateToken(anonymousId, auth);

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) parseClaims(token).get("permissions", List.class);
        assertThat(permissions).containsExactlyInAnyOrder("ROLE_USER", "SURVEY_SUBMIT");
    }

    @Test
    void generateToken_expirationIsInFuture() {
        String token = service.generateToken(UUID.randomUUID(), mockAuth(List.of()));

        Claims claims = parseClaims(token);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    private Authentication mockAuth(List<String> authorities) {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> granted = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
        doReturn(granted).when(auth).getAuthorities();
        return auth;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(verificationKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
