package com.hospital.records.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        /** Optional, defaults to 30 minutes. */
        @Min(value = 5, message = "durationMinutes must be at least 5")
        @Max(value = 240, message = "durationMinutes must be at most 240")
        Integer durationMinutes
) {
}
