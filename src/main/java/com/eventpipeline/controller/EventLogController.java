package com.eventpipeline.controller;

import com.eventpipeline.dto.*;
import com.eventpipeline.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class EventLogController {

    private final EventLogService eventLogService;

    @GetMapping("/event-type-count")
    public List<EventTypeCountDto> eventTypeCount() {
        return eventLogService.getEventTypeCount();
    }

    @GetMapping("/user-event-count")
    public List<UserEventCountDto> userEventCount() {
        return eventLogService.getUserEventCountTop10();
    }

    @GetMapping("/hourly-trend")
    public List<HourlyEventDto> hourlyTrend() {
        return eventLogService.getHourlyTrend();
    }

    @GetMapping("/error-rate")
    public ErrorRateDto errorRate() {
        return eventLogService.getErrorRate();
    }

    @GetMapping("/funnel")
    public FunnelStepDto funnel() {
        return eventLogService.getFunnelStats();
    }

    @GetMapping("/avg-conversion-time")
    public AvgConversionTimeDto avgConversionTime() {
        return eventLogService.getAvgConversionTime();
    }
}
