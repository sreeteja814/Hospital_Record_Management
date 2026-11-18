package com.hospital.records.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** A patient creating their own login. The MRN and date of birth must match the hospital's record. */
public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255)
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password,

        @NotBlank(message = "mrn is required")
        @Size(max = 20)
        String mrn,

        @NotNull(message = "dateOfBirth is required")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth
) {
}
