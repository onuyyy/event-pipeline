package com.eventpipeline.service;

import com.eventpipeline.dto.*;
import com.eventpipeline.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    public List<EventTypeCountDto> getEventTypeCount() {
        return eventLogRepository.countByEventType().stream()
                .map(row -> new EventTypeCountDto(
                        (String) row[0],
                        toLong(row[1])
                ))
                .toList();
    }

    public List<UserEventCountDto> getUserEventCountTop10() {
        return eventLogRepository.countByUserTop10().stream()
                .map(row -> new UserEventCountDto(
                        (String) row[0],
                        toLong(row[1])
                ))
                .toList();
    }

    public List<HourlyEventDto> getHourlyTrend() {
        return eventLogRepository.countByHour().stream()
                .map(row -> new HourlyEventDto(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        toLong(row[1])
                ))
                .toList();
    }

    public ErrorRateDto getErrorRate() {
        Object[] row = eventLogRepository.errorRate();
        return new ErrorRateDto(
                toLong(row[0]),
                toLong(row[1]),
                toDouble(row[2])
        );
    }

    public FunnelStepDto getFunnelStats() {
        Object[] row = eventLogRepository.funnelStats();
        return new FunnelStepDto(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2])
        );
    }

    public AvgConversionTimeDto getAvgConversionTime() {
        Object[] row = eventLogRepository.avgConversionTime();
        return new AvgConversionTimeDto(row[0] != null ? toDouble(row[0]) : 0.0);
    }

    private long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private double toDouble(Object value) {
        return ((Number) value).doubleValue();
    }
}
