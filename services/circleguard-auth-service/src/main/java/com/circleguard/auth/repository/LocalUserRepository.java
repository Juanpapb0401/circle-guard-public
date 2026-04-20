package com.circleguard.auth.repository;

import com.circleguard.auth.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {
    Optional<LocalUser> findByUsername(String username);
}
