package com.hospital.records.dto;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String actorEmail,
        String actorRole,
        AuditAction action,
        String entityType,
        Long entityId,
        String details
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails());
    }
}
