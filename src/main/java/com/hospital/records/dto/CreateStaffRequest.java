package com.hospital.records.dto;

import com.hospital.records.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255)
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password,

        @NotBlank(message = "fullName is required")
        @Size(max = 100)
        String fullName,

        @NotNull(message = "role is required")
        Role role,

        /** Required when role is DOCTOR, ignored otherwise. */
        @Size(max = 100)
        String specialization
) {
}
