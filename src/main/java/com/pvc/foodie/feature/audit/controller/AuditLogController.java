package com.pvc.foodie.feature.audit.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.admin.service.AdminAccessService;
import com.pvc.foodie.feature.audit.dto.AuditLogResponse;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        adminAccessService.requireAdmin();
        return ApiResponse.ok(auditLogService.getAuditLogs(actorId, actor, action, entityType, from, to));
    }
}
