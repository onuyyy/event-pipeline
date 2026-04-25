package com.eventpipeline.generator;

import org.slf4j.MDC;

import java.time.LocalDateTime;

public final class MDCUtil {

    private MDCUtil() {}

    public static void set(String userId, String sessionId, String trafficSource, String deviceType) {
        MDC.put("userId", userId);
        MDC.put("sessionId", sessionId);
        MDC.put("trafficSource", trafficSource);
        MDC.put("deviceType", deviceType);
    }

    public static void setEventTime(LocalDateTime eventTime) {
        MDC.put("eventTime", eventTime.toString());
    }

    public static void clear() {
        MDC.clear();
    }
}
