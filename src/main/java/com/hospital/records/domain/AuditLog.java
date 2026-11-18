package com.hospital.records.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One line of the audit trail. Append-only: there are no setters and no update or delete operations anywhere in the code.
 * It deliberately stores WHO did WHAT to WHICH record, but never clinical content.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false, length = 255)
    private String actorEmail;

    @Column(nullable = false, updatable = false, length = 20)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 20)
    private AuditAction action;

    @Column(nullable = false, updatable = false, length = 30)
    private String entityType;

    @Column(updatable = false)
    private Long entityId;

    @Column(updatable = false, length = 500)
    private String details;

    protected AuditLog() {
        // for JPA
    }

    public AuditLog(String actorEmail, String actorRole, AuditAction action,
                    String entityType, Long entityId, String details) {
        this.occurredAt = Instant.now();
        this.actorEmail = truncate(actorEmail, 255);
        this.actorRole = truncate(actorRole, 20);
        this.action = action;
        this.entityType = truncate(entityType, 30);
        this.entityId = entityId;
        this.details = truncate(details, 500);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
