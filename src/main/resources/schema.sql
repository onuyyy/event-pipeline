CREATE TABLE IF NOT EXISTS event_logs (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(50)  NOT NULL,
    user_id        VARCHAR(100) NOT NULL,
    session_id     VARCHAR(100) NOT NULL,
    event_time     TIMESTAMP    NOT NULL,
    page_url       VARCHAR(255),
    referrer_url   VARCHAR(255),
    traffic_source VARCHAR(50),
    device_type    VARCHAR(20),
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    properties     JSONB
);

CREATE INDEX IF NOT EXISTS idx_event_logs_event_type  ON event_logs (event_type);
CREATE INDEX IF NOT EXISTS idx_event_logs_user_id     ON event_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_event_logs_session_id  ON event_logs (session_id);
CREATE INDEX IF NOT EXISTS idx_event_logs_event_time  ON event_logs (event_time);
