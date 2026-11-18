package com.hospital.records.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hospital.records.dto.ActiveRequest;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.PatientRequest;
import com.hospital.records.dto.PatientResponse;
import com.hospital.records.security.Actor;
import com.hospital.records.service.PatientService;

import jakarta.validation.Valid;

/** Patient demographics (no clinical data). Clinical data lives under MedicalRecordController. */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request,
                                                  Authentication authentication) {
        PatientResponse created = patientService.create(Actor.from(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** GET /api/patients?q=asha&active=true&page=0&size=10&sort=fullName,asc */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public PageResponse<PatientResponse> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "true") boolean active,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {
        return patientService.search(Actor.from(authentication), q, active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public PatientResponse get(@PathVariable Long id, Authentication authentication) {
        return patientService.get(Actor.from(authentication), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public PatientResponse update(@PathVariable Long id,
                                  @Valid @RequestBody PatientRequest request,
                                  Authentication authentication) {
        return patientService.update(Actor.from(authentication), id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public PatientResponse setActive(@PathVariable Long id,
                                     @Valid @RequestBody ActiveRequest request,
                                     Authentication authentication) {
        return patientService.setActive(Actor.from(authentication), id, request.active());
    }
}
