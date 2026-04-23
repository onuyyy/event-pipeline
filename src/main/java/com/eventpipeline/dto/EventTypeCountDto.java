package com.eventpipeline.dto;

public record EventTypeCountDto(
        String eventType,
        long count
) {}
