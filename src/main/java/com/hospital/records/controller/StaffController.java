package com.hospital.records.controller;

import com.hospital.records.domain.Role;
import com.hospital.records.dto.ActiveRequest;
import com.hospital.records.dto.CreateStaffRequest;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.UserResponse;
import com.hospital.records.security.Actor;
import com.hospital.records.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff account management: administrators only. */
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateStaffRequest request, Authentication authentication) {
        return staffService.create(Actor.from(authentication), request);
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        // "fullName" is allowed for sorting, see StaffService
        return staffService.list(role, pageable);
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id,
                                  @Valid @RequestBody ActiveRequest request,
                                  Authentication authentication) {
        return staffService.setActive(Actor.from(authentication), id, request.active());
    }
}
