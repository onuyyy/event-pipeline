package com.eventpipeline.dto;

public record UserEventCountDto(
        String userId,
        long count
) {}
