package com.hospital.records.controller;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.dto.AuditLogResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.service.AuditQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of the audit trail. There is no endpoint to change or delete entries. */
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /** GET /api/admin/audit?entityType=Patient&entityId=3&action=VIEW&actorEmail=dr.rao@hospital.test */
    @GetMapping
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditQueryService.search(actorEmail, action, entityType, entityId, pageable);
    }
}
