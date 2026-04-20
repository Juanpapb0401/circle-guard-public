package com.circleguard.auth.service;

import com.circleguard.auth.model.*;
import com.circleguard.auth.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final LocalUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LocalUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.getIsActive()) {
            throw new DisabledException("User account is disabled");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add roles (prefixed with ROLE_)
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            
            // Add granular permissions
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });

        return new User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
class DisabledException extends RuntimeException {
    public DisabledException(String message) { super(message); }
}
