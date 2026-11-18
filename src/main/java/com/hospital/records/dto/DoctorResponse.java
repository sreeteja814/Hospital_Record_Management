package com.hospital.records.dto;

import com.hospital.records.domain.StaffUser;

/** What the front desk needs to book a doctor. Deliberately leaves out the email and other account details. */
public record DoctorResponse(Long id, String fullName, String specialization) {

    public static DoctorResponse from(StaffUser doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getFullName(), doctor.getSpecialization());
    }
}
