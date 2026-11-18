package com.hospital.records.controller;

import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.dto.AppointmentResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.SelfBookingRequest;
import com.hospital.records.security.Actor;
import com.hospital.records.service.PatientPortalService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** The patient's own area. Everything here is scoped to the logged-in patient; there is no patientId to tamper with. */
@RestController
@RequestMapping("/api/me/appointments")
@PreAuthorize("hasRole('PATIENT')")
public class PatientPortalController {

    private final PatientPortalService portal;

    public PatientPortalController(PatientPortalService portal) {
        this.portal = portal;
    }

    @GetMapping
    public PageResponse<AppointmentResponse> mine(
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {
        return portal.myAppointments(Actor.from(authentication), status, pageable);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody SelfBookingRequest request,
                                                    Authentication authentication) {
        AppointmentResponse created = portal.book(Actor.from(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, Authentication authentication) {
        return portal.cancel(Actor.from(authentication), id);
    }
}
