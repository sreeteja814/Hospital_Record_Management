package com.hospital.records.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull(message = "patientId is required")
        Long patientId,

        @NotNull(message = "doctorId is required")
        Long doctorId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        /** Optional, defaults to 30 minutes. */
        @Min(value = 5, message = "durationMinutes must be at least 5")
        @Max(value = 240, message = "durationMinutes must be at most 240")
        Integer durationMinutes,

        @Size(max = 255)
        String reason
) {
}
