# Event Pipeline — 라이브클래스 사용자 행동 로그 파이프라인

> **채용 과제 제출용** | 플랫폼/데이터 엔지니어링 인턴

---

## 프로젝트 개요

라이브클래스(강의 플랫폼)에서 발생하는 사용자 행동 이벤트를 수집·저장·분석·시각화하는 경량 데이터 파이프라인입니다.

- **이벤트 생성** → **PostgreSQL 저장** → **집계 쿼리 분석** → **Grafana 시각화**
- Spring Boot AOP 기반으로 비즈니스 코드 변경 없이 이벤트를 자동 수집합니다.

---

## 이벤트 설계 이유

강의 플랫폼의 핵심 사용자 퍼널은 **조회 → 재생 → 결제**입니다. 이 퍼널의 각 단계와 예외 상황을 커버하도록 4가지 이벤트를 설계했습니다.

| 이벤트 타입 | 퍼널 단계 | 의미 |
|---|---|---|
| `LECTURE_VIEW` | 조회 | 강의 상세 페이지 진입 — 관심 측정 |
| `LECTURE_PLAY` | 재생 | 수강 시작 — 실제 학습 전환율 측정 |
| `ENROLLMENT` | 결제 | 수강 신청/결제 완료 — 최종 전환 |
| `ERROR` | 예외 | 재생 실패, 결제 오류 등 — 이탈 원인 분석 |

`LECTURE_VIEW → LECTURE_PLAY` 전환율로 콘텐츠 매력도를, `LECTURE_PLAY → ENROLLMENT` 전환율로 구매 의향을 추적할 수 있습니다. `ERROR` 이벤트는 퍼널 이탈 원인을 특정하는 데 활용합니다.

---

## DB 스키마

### `event_logs` 테이블

```sql
CREATE TABLE event_logs (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL,          -- LECTURE_VIEW | LECTURE_PLAY | ENROLLMENT | ERROR
    user_id     VARCHAR(100) NOT NULL,           -- 사용자 식별자
    session_id  VARCHAR(100),                    -- 세션 단위 퍼널 추적
    status      VARCHAR(20)  NOT NULL,           -- SUCCESS | FAILURE
    properties  JSONB,                           -- 이벤트별 가변 속성 (lecture_id, error_code 등)
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_logs_event_type ON event_logs(event_type);
CREATE INDEX idx_event_logs_user_id    ON event_logs(user_id);
CREATE INDEX idx_event_logs_created_at ON event_logs(created_at);
```

**스키마 설계 이유:** 모든 이벤트에 공통되는 필드(event_type, user_id, status, created_at)는 전용 컬럼으로 분리해 인덱싱과 집계 쿼리 성능을 확보했습니다. 이벤트마다 다른 가변 속성(강의 ID, 에러 코드 등)은 JSONB로 저장해 스키마 변경 없이 새 필드를 추가할 수 있도록 했습니다.

---

## 저장소 선택 이유 — PostgreSQL + JSONB

| 기준 | 선택 이유 |
|---|---|
| **JSONB 지원** | 이벤트별 가변 속성을 스키마 변경 없이 저장하면서, JSON 내부 필드에도 인덱스를 걸 수 있음 |
| **집계 성능** | `GROUP BY`, `DATE_TRUNC` 등 분석 쿼리를 SQL로 간결하게 표현 가능 |
| **Grafana 연동** | Grafana PostgreSQL 데이터소스로 추가 ETL 없이 직접 시각화 가능 |
| **Docker 친화성** | 공식 이미지로 Compose 구성이 단순함 |

순수 파일(JSON/CSV) 저장 방식은 집계 쿼리와 인덱싱이 불가능하고, MongoDB 등 NoSQL은 Grafana 연동에 추가 플러그인이 필요해 제외했습니다.

---

## 아키텍처 흐름

```
┌─────────────────────────────────────────────────────┐
│                   Docker Compose                    │
│                                                     │
│  ┌──────────────────────────┐                       │
│  │     Spring Boot App      │                       │
│  │                          │                       │
│  │  EventGenerator          │                       │
│  │    (랜덤 이벤트 생성)      │                       │
│  │        │                 │                       │
│  │        ▼                 │                       │
│  │  @UserEvent AOP          │                       │
│  │    (자동 인터셉트)         │                       │
│  │        │                 │                       │
│  │        ▼                 │                       │
│  │  ApplicationEventPublisher│                      │
│  │        │                 │                       │
│  │        ▼ (@Async)        │                       │
│  │  EventLogService         │                       │
│  │    (저장 처리)             │                       │
│  │        │                 │                       │
│  └────────┼─────────────────┘                       │
│           │                                         │
│           ▼                                         │
│  ┌─────────────────┐    ┌────────────────────┐      │
│  │   PostgreSQL    │◄───│  집계 쿼리 (분석)    │      │
│  │  event_logs     │    └────────────────────┘      │
│  └────────┬────────┘                                │
│           │                                         │
│           ▼                                         │
│  ┌─────────────────┐                                │
│  │    Grafana      │  (대시보드 시각화)               │
│  └─────────────────┘                                │
└─────────────────────────────────────────────────────┘
```

---

## 실행 방법

### 필요 도구
- Docker Desktop (Docker Compose 포함)

### 실행

```bash
# 1. 저장소 클론
git clone https://github.com/{username}/event-pipeline.git
cd event-pipeline

# 2. 전체 스택 실행 (앱 + PostgreSQL + Grafana)
docker compose up --build

# 앱 시작 후 이벤트 자동 생성 → PostgreSQL 저장까지 자동 동작
```

### 접속 정보

| 서비스 | URL | 계정 |
|---|---|---|
| Spring Boot API | http://localhost:8080 | - |
| Grafana | http://localhost:3000 | admin / admin |

### 집계 쿼리 직접 실행

```bash
docker compose exec postgres psql -U pipeline -d eventdb
```

---

## 구현하면서 고민한 점

*(Step 7에서 작성 예정)*

---

## AI-Assisted Development

이 프로젝트는 Claude Code (claude-sonnet-4-6)를 활용해 개발되었습니다.

### CLAUDE.md 활용 방식
프로젝트 루트의 `CLAUDE.md`에 아키텍처 규칙, 네이밍 컨벤션, 금지 패턴을 선언형으로 정의했습니다. Claude Code는 매 작업 시 이 파일을 컨텍스트로 참조해 일관된 코드 스타일을 유지합니다. 예를 들어 새 이벤트 타입을 추가할 때 "EventType enum에만 추가" 규칙이 적용되어 흩어진 하드코딩을 방지합니다.

### 아키텍처 결정에 AI 협의 활용
- **AOP vs 직접 호출 방식**: 비즈니스 코드 침투 최소화를 위해 AOP + `@UserEvent` 어노테이션 방식을 채택하는 과정에서 트레이드오프를 AI와 함께 검토했습니다.
- **JSONB vs 별도 테이블**: 이벤트별 가변 속성 저장 전략에서 확장성과 쿼리 성능 간 균형을 논의해 JSONB 컬럼 방식을 선택했습니다.
- **동기 vs 비동기 저장**: `@Async` EventListener를 적용해 이벤트 저장이 원본 트랜잭션을 블로킹하지 않도록 설계했습니다.