package com.eventpipeline.aspect;

import com.eventpipeline.annotation.UserEvent;
import com.eventpipeline.domain.EventStatus;
import com.eventpipeline.event.UserEventPayload;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class UserEventAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(userEvent)")
    public Object handle(ProceedingJoinPoint joinPoint, UserEvent userEvent) throws Throwable {
        String userId        = mdcOrDefault("userId",        "anonymous");
        String sessionId     = mdcOrDefault("sessionId",     "no-session");
        String trafficSource = mdcOrDefault("trafficSource", "direct");
        String deviceType    = mdcOrDefault("deviceType",    "pc");

        Map<String, Object> properties = extractProperties(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            publish(userEvent, userId, sessionId, trafficSource, deviceType, EventStatus.SUCCESS, properties);
            return result;
        } catch (Throwable e) {
            publish(userEvent, userId, sessionId, trafficSource, deviceType, EventStatus.FAILURE, properties);
            throw e;
        }
    }

    private void publish(UserEvent userEvent, String userId, String sessionId,
                         String trafficSource, String deviceType,
                         EventStatus status, Map<String, Object> properties) {
        eventPublisher.publishEvent(UserEventPayload.builder()
                .eventType(userEvent.type())
                .userId(userId)
                .sessionId(sessionId)
                .eventTime(LocalDateTime.now())
                .trafficSource(trafficSource)
                .deviceType(deviceType)
                .status(status)
                .properties(properties)
                .build());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractProperties(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Map) return (Map<String, Object>) arg;
        }
        return new HashMap<>();
    }

    private String mdcOrDefault(String key, String defaultValue) {
        String value = MDC.get(key);
        return value != null ? value : defaultValue;
    }
}
