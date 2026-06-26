package com.pvc.foodie.feature.admin.dto;

public record AdminPaymentTimelineEntry(
        String label,
        boolean complete,
        String reference) {
}
