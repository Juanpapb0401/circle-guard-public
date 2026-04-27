package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceUnitTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hashSalt", "test-salt");
    }

    @Test
    void getOrCreateAnonymousId_existingIdentity_doesNotSaveAgain() {
        UUID fixedId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(fixedId)
                .realIdentity("juan@universidad.edu")
                .identityHash("some-hash")
                .salt("some-salt")
                .build();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(existing));

        UUID result = service.getOrCreateAnonymousId("juan@universidad.edu");

        assertThat(result).isEqualTo(fixedId);
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreateAnonymousId_newIdentity_savesAndReturnsId() {
        UUID newId = UUID.randomUUID();
        IdentityMapping saved = IdentityMapping.builder()
                .anonymousId(newId)
                .realIdentity("nuevo@universidad.edu")
                .identityHash("hash")
                .salt("salt")
                .build();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(saved);

        UUID result = service.getOrCreateAnonymousId("nuevo@universidad.edu");

        assertThat(result).isEqualTo(newId);
        verify(repository).save(any(IdentityMapping.class));
    }

    @Test
    void resolveRealIdentity_unknownId_throws404() {
        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveRealIdentity(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Identity not found");
    }
}
