package com.hospital.records.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordRequest(
        @NotBlank(message = "diagnosis is required")
        @Size(max = 500, message = "diagnosis must be at most 500 characters")
        String diagnosis,

        @Size(max = 2000, message = "treatment must be at most 2000 characters")
        String treatment,

        @Size(max = 2000, message = "prescription must be at most 2000 characters")
        String prescription,

        @Size(max = 4000, message = "notes must be at most 4000 characters")
        String notes,

        /** Ignored when creating a record; required when editing one (it is stored in the version history). */
        @Size(max = 255, message = "changeReason must be at most 255 characters")
        String changeReason
) {
}
