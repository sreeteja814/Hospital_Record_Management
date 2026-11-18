package com.hospital.records.service;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.AuditLog;
import com.hospital.records.repository.AuditLogRepository;
import com.hospital.records.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the audit trail. Every entry is saved in its OWN transaction (REQUIRES_NEW), for two reasons:
 * reads (which run in read-only transactions) can still be logged, and an entry is kept even if the
 * business operation fails afterwards. Over-reporting an attempt is safer than losing the trace of one.
 * Never put clinical content (diagnosis, notes, ...) in {@code details}.
 */
@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Actor actor, AuditAction action, String entityType, Long entityId, String details) {
        logs.save(new AuditLog(actor.email(), actor.role().name(), action, entityType, entityId, details));
    }

    /** For events without a logged-in staff member, such as a failed login. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAnonymous(String actorEmail, AuditAction action, String entityType, Long entityId, String details) {
        logs.save(new AuditLog(actorEmail, "ANONYMOUS", action, entityType, entityId, details));
    }
}
