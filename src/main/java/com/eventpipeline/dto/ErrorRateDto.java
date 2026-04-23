package com.eventpipeline.dto;

public record ErrorRateDto(
        long errorCount,
        long totalCount,
        double errorRate
) {}
