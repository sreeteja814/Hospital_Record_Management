package com.hospital.records.controller;

import com.hospital.records.dto.DoctorResponse;
import com.hospital.records.service.StaffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST','PATIENT')")
public class DoctorController {

    private final StaffService staffService;

    public DoctorController(StaffService staffService) {
        this.staffService = staffService;
    }

    /** Active doctors, for choosing who to book. */
    @GetMapping
    public List<DoctorResponse> list() {
        return staffService.activeDoctors();
    }
}
