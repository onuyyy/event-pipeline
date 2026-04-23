package com.eventpipeline.dto;

import java.time.LocalDateTime;

public record HourlyEventDto(
        LocalDateTime hour,
        long count
) {}
