package com.circleguard.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IdentityVaultServiceIntegrationTest {

    @Autowired
    private IdentityVaultService service;

    @Test
    void getOrCreateAnonymousId_sameIdentityTwice_returnsSameUuid() {
        String identity = "juan@universidad.edu";

        UUID first  = service.getOrCreateAnonymousId(identity);
        UUID second = service.getOrCreateAnonymousId(identity);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void getOrCreateAnonymousId_differentIdentities_returnDifferentUuids() {
        UUID idA = service.getOrCreateAnonymousId("alice@universidad.edu");
        UUID idB = service.getOrCreateAnonymousId("bob@universidad.edu");

        assertThat(idA).isNotEqualTo(idB);
    }

    @Test
    void resolveRealIdentity_afterCreation_returnsOriginalIdentity() {
        String identity = "carol@universidad.edu";
        UUID anonymousId = service.getOrCreateAnonymousId(identity);

        String resolved = service.resolveRealIdentity(anonymousId);

        assertThat(resolved).isEqualTo(identity);
    }
}
