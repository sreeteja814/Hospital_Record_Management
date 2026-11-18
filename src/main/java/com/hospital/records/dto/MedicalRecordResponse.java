package com.hospital.records.dto;

import com.hospital.records.domain.MedicalRecord;

import java.time.Instant;

public record MedicalRecordResponse(
        Long id,
        Long patientId,
        Long doctorId,
        String doctorName,
        String diagnosis,
        String treatment,
        String prescription,
        String notes,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt
) {

    public static MedicalRecordResponse from(MedicalRecord r) {
        return new MedicalRecordResponse(
                r.getId(),
                r.getPatient().getId(),
                r.getDoctor().getId(),
                r.getDoctor().getFullName(),
                r.getDiagnosis(),
                r.getTreatment(),
                r.getPrescription(),
                r.getNotes(),
                r.getCurrentVersion(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
