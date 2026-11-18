package com.hospital.records.dto;

import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String specialization,
        Long patientId,
        boolean active,
        Instant createdAt
) {

    public static UserResponse from(StaffUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getSpecialization(),
                user.getPatientId(),
                user.isActive(),
                user.getCreatedAt());
    }
}
