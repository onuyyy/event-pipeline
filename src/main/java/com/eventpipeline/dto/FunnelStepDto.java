package com.eventpipeline.dto;

public record FunnelStepDto(
        long productViewSessions,
        long addToCartSessions,
        long purchaseSessions
) {}
