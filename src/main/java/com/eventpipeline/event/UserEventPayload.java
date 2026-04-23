package com.eventpipeline.event;

import com.eventpipeline.domain.EventStatus;
import com.eventpipeline.domain.EventType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record UserEventPayload(
        EventType eventType,
        String userId,
        String sessionId,
        LocalDateTime eventTime,
        String trafficSource,
        String deviceType,
        EventStatus status,
        Map<String, Object> properties
) {}
