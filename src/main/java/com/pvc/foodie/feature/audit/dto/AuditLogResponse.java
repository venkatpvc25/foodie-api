package com.pvc.foodie.feature.audit.dto;

import java.time.Instant;
import java.util.UUID;

import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String actorEmail,
        AuditAction action,
        AuditEntityType entityType,
        UUID entityId,
        String entityLabel,
        String details,
        Instant createdAt) {
}
