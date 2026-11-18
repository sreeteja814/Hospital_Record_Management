package com.hospital.records.service;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.dto.AuthResponse;
import com.hospital.records.dto.LoginRequest;
import com.hospital.records.dto.UserResponse;
import com.hospital.records.exception.ResourceNotFoundException;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import com.hospital.records.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final StaffUserRepository users;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService audit;

    public AuthService(StaffUserRepository users,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditService audit) {
        this.users = users;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.audit = audit;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            audit.recordAnonymous(email, AuditAction.LOGIN_FAILED, "StaffUser", null, "Failed login attempt");
            throw ex; // GlobalExceptionHandler turns this into a 401
        }

        StaffUser user = users.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        audit.record(new Actor(user.getEmail(), user.getRole()), AuditAction.LOGIN, "StaffUser", user.getId(), null);

        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                "Bearer",
                jwtService.getLifetimeSeconds(),
                user.getRole().name());
    }

    public UserResponse me(String email) {
        return users.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
