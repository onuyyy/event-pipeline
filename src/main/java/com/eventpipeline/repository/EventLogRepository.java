package com.eventpipeline.repository;

import com.eventpipeline.domain.EventLog;
import com.eventpipeline.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // 이벤트 타입별 발생 횟수
    @Query(value = """
            SELECT event_type, COUNT(*) AS count
            FROM event_logs
            GROUP BY event_type
            ORDER BY count DESC
            """, nativeQuery = true)
    List<Object[]> countByEventType();

    // 유저별 총 이벤트 수 Top 10
    @Query(value = """
            SELECT user_id, COUNT(*) AS count
            FROM event_logs
            GROUP BY user_id
            ORDER BY count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> countByUserTop10();

    // 시간대별 이벤트 추이
    @Query(value = """
            SELECT DATE_TRUNC('hour', event_time) AS hour, COUNT(*) AS count
            FROM event_logs
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> countByHour();

    // 에러 이벤트 비율
    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE status = 'FAILURE')          AS error_count,
                COUNT(*)                                             AS total_count,
                ROUND(
                    COUNT(*) FILTER (WHERE status = 'FAILURE') * 100.0
                    / NULLIF(COUNT(*), 0), 2
                )                                                    AS error_rate
            FROM event_logs
            """, nativeQuery = true)
    Object[] errorRate();

    // 퍼널 단계별 세션 수
    @Query(value = """
            SELECT
                COUNT(DISTINCT CASE WHEN event_type = 'PRODUCT_VIEW'      THEN session_id END) AS product_view_sessions,
                COUNT(DISTINCT CASE WHEN event_type = 'ADD_TO_CART'       THEN session_id END) AS add_to_cart_sessions,
                COUNT(DISTINCT CASE WHEN event_type = 'PURCHASE_COMPLETED' THEN session_id END) AS purchase_sessions
            FROM event_logs
            """, nativeQuery = true)
    Object[] funnelStats();

    // 구매까지 평균 소요 시간 (분)
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (purchase_time - view_time)) / 60) AS avg_minutes
            FROM (
                SELECT
                    session_id,
                    MIN(CASE WHEN event_type = 'PRODUCT_VIEW'       THEN event_time END) AS view_time,
                    MAX(CASE WHEN event_type = 'PURCHASE_COMPLETED'  THEN event_time END) AS purchase_time
                FROM event_logs
                GROUP BY session_id
                HAVING MIN(CASE WHEN event_type = 'PRODUCT_VIEW'       THEN event_time END) IS NOT NULL
                   AND MAX(CASE WHEN event_type = 'PURCHASE_COMPLETED'  THEN event_time END) IS NOT NULL
            ) AS session_times
            """, nativeQuery = true)
    Object[] avgConversionTime();
}
