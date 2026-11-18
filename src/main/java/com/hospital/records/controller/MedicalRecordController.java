package com.hospital.records.controller;

import com.hospital.records.dto.MedicalRecordRequest;
import com.hospital.records.dto.MedicalRecordResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.VersionResponse;
import com.hospital.records.security.Actor;
import com.hospital.records.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Clinical data. Doctors read and write; nurses read only; admins and receptionists get a 403. */
@RestController
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    public MedicalRecordController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/api/patients/{patientId}/records")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecordResponse> create(@PathVariable Long patientId,
                                                        @Valid @RequestBody MedicalRecordRequest request,
                                                        Authentication authentication) {
        MedicalRecordResponse created = recordService.create(Actor.from(authentication), patientId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/records/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/patients/{patientId}/records")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public PageResponse<MedicalRecordResponse> listForPatient(
            @PathVariable Long patientId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return recordService.listForPatient(Actor.from(authentication), patientId, pageable);
    }

    @GetMapping("/api/records/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public MedicalRecordResponse get(@PathVariable Long id, Authentication authentication) {
        return recordService.get(Actor.from(authentication), id);
    }

    @PutMapping("/api/records/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public MedicalRecordResponse update(@PathVariable Long id,
                                        @Valid @RequestBody MedicalRecordRequest request,
                                        Authentication authentication) {
        return recordService.update(Actor.from(authentication), id, request);
    }

    @GetMapping("/api/records/{id}/history")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public List<VersionResponse> history(@PathVariable Long id, Authentication authentication) {
        return recordService.history(Actor.from(authentication), id);
    }
}
