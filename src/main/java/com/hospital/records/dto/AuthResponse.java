package com.hospital.records.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, String role) {
}
