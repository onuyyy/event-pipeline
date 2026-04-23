package com.eventpipeline.repository;

import com.eventpipeline.domain.EventLog;
import com.eventpipeline.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    List<EventLog> findByUserIdOrderByEventTimeAsc(String userId);

    List<EventLog> findBySessionIdOrderByEventTimeAsc(String sessionId);

    List<EventLog> findByEventTypeAndEventTimeBetween(
            EventType eventType,
            LocalDateTime from,
            LocalDateTime to
    );
}
