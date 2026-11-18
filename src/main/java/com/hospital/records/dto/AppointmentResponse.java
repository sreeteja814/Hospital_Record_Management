package com.hospital.records.dto;

import com.hospital.records.domain.Appointment;
import com.hospital.records.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        String patientMrn,
        Long doctorId,
        String doctorName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String reason
) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFullName(),
                a.getPatient().getMrn(),
                a.getDoctor().getId(),
                a.getDoctor().getFullName(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getReason());
    }
}
