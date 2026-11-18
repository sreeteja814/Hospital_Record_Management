package com.hospital.records.dto;

import com.hospital.records.domain.Gender;
import com.hospital.records.domain.Patient;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String mrn,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        String address,
        String bloodGroup,
        String emergencyContactName,
        String emergencyContactPhone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatientResponse from(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getMrn(),
                p.getFullName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getPhone(),
                p.getEmail(),
                p.getAddress(),
                p.getBloodGroup(),
                p.getEmergencyContactName(),
                p.getEmergencyContactPhone(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
