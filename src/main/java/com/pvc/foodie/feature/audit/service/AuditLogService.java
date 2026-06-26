package com.pvc.foodie.feature.audit.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.feature.audit.dto.AuditLogResponse;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.entity.AuditLog;
import com.pvc.foodie.feature.audit.repository.AuditLogRepository;
import com.pvc.foodie.feature.auth.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(
            UUID actorId,
            String actor,
            AuditAction action,
            AuditEntityType entityType,
            Instant from,
            Instant to) {
        return auditLogRepository.findAll().stream()
                .filter(log -> actorId == null || (log.getActor() != null && log.getActor().getId().equals(actorId)))
                .filter(log -> matchesActor(log, actor))
                .filter(log -> action == null || log.getAction() == action)
                .filter(log -> entityType == null || log.getEntityType() == entityType)
                .filter(log -> from == null || !log.getCreatedAt().isBefore(from))
                .filter(log -> to == null || !log.getCreatedAt().isAfter(to))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void record(
            User actor,
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            String entityLabel,
            String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setActorName(actor == null ? null : actor.getName());
        log.setActorEmail(actor == null ? null : actor.getEmail());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setEntityLabel(entityLabel);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private boolean matchesActor(AuditLog log, String actor) {
        if (actor == null || actor.isBlank()) {
            return true;
        }
        String normalized = actor.toLowerCase();
        return contains(log.getActorName(), normalized) || contains(log.getActorEmail(), normalized);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() == null ? null : log.getActor().getId(),
                log.getActorName(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getEntityLabel(),
                log.getDetails(),
                log.getCreatedAt());
    }
}
