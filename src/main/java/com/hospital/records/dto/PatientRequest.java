package com.hospital.records.dto;

import com.hospital.records.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must be at most 100 characters")
        String fullName,

        @NotNull(message = "dateOfBirth is required")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "gender is required")
        Gender gender,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "phone must be 7 to 20 digits (spaces, + and - allowed)")
        String phone,

        @Email(message = "email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 255)
        String address,

        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "bloodGroup must look like A+, O-, AB+")
        String bloodGroup,

        @Size(max = 100)
        String emergencyContactName,

        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "emergencyContactPhone must be 7 to 20 digits")
        String emergencyContactPhone
) {
}
