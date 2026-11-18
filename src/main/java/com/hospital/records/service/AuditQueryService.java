package com.hospital.records.service;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.AuditLog;
import com.hospital.records.dto.AuditLogResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.repository.AuditLogRepository;
import com.hospital.records.util.SortValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class AuditQueryService {

    private static final Set<String> SORTABLE = Set.of("id", "occurredAt");

    private final AuditLogRepository logs;

    public AuditQueryService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String actorEmail,
                                                 AuditAction action,
                                                 String entityType,
                                                 Long entityId,
                                                 Pageable pageable) {
        SortValidator.require(pageable, SORTABLE);

        Specification<AuditLog> spec = (root, cq, cb) -> cb.conjunction();
        if (actorEmail != null && !actorEmail.isBlank()) {
            String email = actorEmail.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, cq, cb) -> cb.equal(cb.lower(root.get("actorEmail")), email));
        }
        if (action != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("action"), action));
        }
        if (entityType != null && !entityType.isBlank()) {
            String type = entityType.trim();
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("entityType"), type));
        }
        if (entityId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("entityId"), entityId));
        }

        return PageResponse.from(logs.findAll(spec, pageable).map(AuditLogResponse::from));
    }
}
