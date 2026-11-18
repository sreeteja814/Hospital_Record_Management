package com.hospital.records.dto;

import com.hospital.records.domain.MedicalRecordVersion;

import java.time.Instant;

public record VersionResponse(
        int versionNumber,
        String diagnosis,
        String treatment,
        String prescription,
        String notes,
        String editedBy,
        Instant editedAt,
        String changeReason
) {

    public static VersionResponse from(MedicalRecordVersion v) {
        return new VersionResponse(
                v.getVersionNumber(),
                v.getDiagnosis(),
                v.getTreatment(),
                v.getPrescription(),
                v.getNotes(),
                v.getEditedBy().getFullName(),
                v.getEditedAt(),
                v.getChangeReason());
    }
}
