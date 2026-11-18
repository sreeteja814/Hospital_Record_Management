package com.hospital.records.security;

import com.hospital.records.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Who is making the current request: the logged-in staff member's email and role. */
public record Actor(String email, Role role) {

    public static Actor from(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));
        return new Actor(authentication.getName(), Role.valueOf(authority.substring("ROLE_".length())));
    }

    public boolean is(Role candidate) {
        return role == candidate;
    }
}
