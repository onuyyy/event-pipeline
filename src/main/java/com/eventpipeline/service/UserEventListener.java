package com.eventpipeline.service;

import com.eventpipeline.domain.EventLog;
import com.eventpipeline.event.UserEventPayload;
import com.eventpipeline.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EventLogRepository eventLogRepository;

    @EventListener
    public void handle(UserEventPayload payload) {
        Map<String, Object> properties = payload.properties();

        EventLog eventLog = EventLog.builder()
                .eventType(payload.eventType())
                .userId(payload.userId())
                .sessionId(payload.sessionId())
                .eventTime(payload.eventTime())
                .trafficSource(payload.trafficSource())
                .deviceType(payload.deviceType())
                .status(payload.status())
                .pageUrl(extractString(properties, "page_url"))
                .referrerUrl(extractString(properties, "referrer_url"))
                .properties(properties)
                .build();

        eventLogRepository.save(eventLog);
    }

    private String extractString(Map<String, Object> properties, String key) {
        if (properties == null) return null;
        Object value = properties.get(key);
        return value instanceof String s ? s : null;
    }
}
