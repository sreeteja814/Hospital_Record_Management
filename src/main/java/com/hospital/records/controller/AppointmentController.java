package com.hospital.records.controller;

import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.dto.AppointmentRequest;
import com.hospital.records.dto.AppointmentResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.RescheduleRequest;
import com.hospital.records.security.Actor;
import com.hospital.records.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> schedule(@Valid @RequestBody AppointmentRequest request,
                                                        Authentication authentication) {
        AppointmentResponse created = appointmentService.schedule(Actor.from(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** GET /api/appointments?doctorId=2&date=2026-11-05&status=SCHEDULED. Doctors only see their own. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public PageResponse<AppointmentResponse> search(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {
        return appointmentService.search(Actor.from(authentication), doctorId, patientId, status, date, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')")
    public AppointmentResponse get(@PathVariable Long id, Authentication authentication) {
        return appointmentService.get(Actor.from(authentication), id);
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public AppointmentResponse reschedule(@PathVariable Long id,
                                          @Valid @RequestBody RescheduleRequest request,
                                          Authentication authentication) {
        return appointmentService.reschedule(Actor.from(authentication), id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public AppointmentResponse cancel(@PathVariable Long id, Authentication authentication) {
        return appointmentService.cancel(Actor.from(authentication), id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse complete(@PathVariable Long id, Authentication authentication) {
        return appointmentService.complete(Actor.from(authentication), id);
    }
}
