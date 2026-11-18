package com.hospital.records.controller;

import com.hospital.records.dto.AuthResponse;
import com.hospital.records.dto.LoginRequest;
import com.hospital.records.dto.RegisterRequest;
import com.hospital.records.dto.UserResponse;
import com.hospital.records.service.AuthService;
import com.hospital.records.service.PatientPortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PatientPortalService patientPortal;

    public AuthController(AuthService authService, PatientPortalService patientPortal) {
        this.authService = authService;
        this.patientPortal = patientPortal;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Public: an existing patient creates their own login (verified by MRN + date of birth). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return patientPortal.register(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
