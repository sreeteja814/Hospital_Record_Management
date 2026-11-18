package com.hospital.records.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** A patient booking for themselves: no patientId, it always comes from their login. */
public record SelfBookingRequest(
        @NotNull(message = "doctorId is required")
        Long doctorId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @Size(max = 255)
        String reason
) {
}
