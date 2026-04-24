# Event Pipeline — 라이브클래스 사용자 행동 로그 파이프라인

> **채용 과제 제출용** | 플랫폼/데이터 엔지니어링 인턴

---

## 프로젝트 개요

라이브클래스(강의 플랫폼)에서 발생하는 사용자 행동 이벤트를 수집·저장·분석·시각화하는 경량 데이터 파이프라인입니다.

- **이벤트 생성** → **PostgreSQL 저장** → **집계 쿼리 분석** → **Grafana 시각화**
- Spring Boot AOP 기반으로 비즈니스 코드 변경 없이 이벤트를 자동 수집합니다.

---

## 이벤트 설계 이유

이커머스의 핵심 사용자 퍼널인 **상품 조회 → 장바구니 → 결제**를 기준으로 4가지 이벤트를 설계했습니다.

| 이벤트 타입 | 퍼널 단계 | 의미 |
|---|---|---|
| `PRODUCT_VIEW` | 조회 | 상품 상세 페이지 진입 — 관심 측정 |
| `ADD_TO_CART` | 장바구니 | 구매 의향 표시 — 중간 전환율 측정 |
| `PURCHASE_COMPLETED` | 결제 | 최종 전환 완료 |
| `ERROR_OCCURRED` | 예외 | 조회/장바구니/결제 중 발생한 오류 — 이탈 원인 분석 |

`PRODUCT_VIEW → ADD_TO_CART` 전환율로 콘텐츠 매력도를, `ADD_TO_CART → PURCHASE_COMPLETED` 전환율로 구매 완주율을 추적할 수 있습니다. `ERROR_OCCURRED` 이벤트는 어느 단계에서 이탈이 발생했는지 특정하는 데 활용합니다.

---

## DB 스키마

### `event_logs` 테이블

```sql
CREATE TABLE event_logs (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(50)  NOT NULL,   -- PRODUCT_VIEW | ADD_TO_CART | PURCHASE_COMPLETED | ERROR_OCCURRED
    user_id        VARCHAR(100) NOT NULL,   -- 사용자 식별자
    session_id     VARCHAR(100) NOT NULL,   -- 세션 단위 퍼널 추적
    event_time     TIMESTAMP    NOT NULL,   -- 실제 이벤트 발생 시각
    traffic_source VARCHAR(50),             -- 유입 경로 (google, direct, ad)
    device_type    VARCHAR(20),             -- 기기 유형 (mobile, pc)
    status         VARCHAR(20)  NOT NULL,   -- SUCCESS | FAILURE
    properties     JSONB,                   -- 이벤트별 가변 속성 (product_id, error_code 등)
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_logs_event_type ON event_logs(event_type);
CREATE INDEX idx_event_logs_user_id    ON event_logs(user_id);
CREATE INDEX idx_event_logs_session_id ON event_logs(session_id);
CREATE INDEX idx_event_logs_event_time ON event_logs(event_time);
```

**스키마 설계 이유:** 집계 쿼리에서 `GROUP BY`, `WHERE`, `DATE_TRUNC` 등으로 직접 쓰이는 필드(event_type, user_id, session_id, event_time)는 전용 컬럼으로 분리해 인덱싱 성능을 확보했습니다. 이벤트 타입마다 구조가 다른 가변 속성(product_id, error_code, payment_method 등)은 JSONB로 저장해 스키마 변경 없이 새 필드를 추가할 수 있도록 했습니다.

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
│  │        ▼                 │                       │
│  │  EventListener           │                       │
│  │    (이벤트 저장)           │                       │
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
git clone https://github.com/onuyyy/event-pipeline.git
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
docker compose exec postgres psql -U eventpipeline -d eventpipeline
```

---

## 구현하면서 고민한 점

**1. 어떤 필드를 고정 컬럼으로, 어떤 걸 JSONB로 넣을지**

이벤트 타입마다 속성이 달라서 모든 필드를 컬럼으로 만들면 대부분의 행에서 NULL이 많아집니다. 반대로 전부 JSONB에 넣으면 `GROUP BY event_type`이나 `WHERE user_id = ?` 같은 집계 쿼리에서 인덱스를 쓸 수 없습니다.

기준을 하나로 정했습니다. **집계 쿼리에서 직접 쓰이면 전용 컬럼, 그렇지 않으면 JSONB.** `event_type`, `user_id`, `session_id`, `event_time`은 항상 GROUP BY나 WHERE에 쓰이니 컬럼으로, `product_id`, `error_code`, `payment_method` 등 이벤트별 가변 속성은 JSONB에 넣었습니다.

**2. 이벤트 수집에 AOP를 선택한 이유**

처음에는 Generator에서 `ApplicationEventPublisher.publishEvent()`를 직접 호출했는데, 이 방식이면 이벤트를 수집하고 싶은 모든 지점에 발행 코드가 흩어집니다. 실서비스라면 비즈니스 메서드마다 발행 로직이 섞여 수정 범위가 넓어질 것이라 판단했습니다.

`@UserEvent` 어노테이션 + AOP로 바꾸면 수집 로직이 `UserEventAspect` 한 곳에 모이고, 비즈니스 코드는 어노테이션 하나만 붙이면 됩니다. 실제로 개발 중에 Generator가 AOP를 우회하고 있다는 걸 발견해 흐름을 바로잡기도 했습니다.

**3. 동기 처리로 구현한 이유**

실서비스라면 이벤트 저장을 Kafka 같은 메시지 큐로 분리해 비동기 처리하는 게 맞습니다. DB 쓰기 부하가 비즈니스 로직에 영향을 주지 않도록 격리할 수 있기 때문입니다.

과제에서는 파이프라인 전체 흐름(생성 → 발행 → 저장 → 조회)을 명확하게 보여주는 것이 목적이라 동기 처리를 선택했습니다. 비동기 확장이 필요하다면 `@EventListener`에 `@Async`를 붙이거나 중간에 큐를 끼우는 방식으로 확장할 수 있습니다.

**4. Spring Events(`ApplicationEventPublisher`)를 쓴 이유**

Generator에서 Repository를 직접 호출하는 방식도 가능하지만, 그러면 저장 방식이 바뀔 때(예: DB → 파일, DB → 외부 API) Generator 코드도 함께 수정해야 합니다. `ApplicationEventPublisher`를 사이에 두면 발행자(Generator)와 저장 로직(Listener)이 분리되어 각자 독립적으로 변경할 수 있습니다.

---

## AI-Assisted Development

이 프로젝트는 Claude Code (claude-sonnet-4-6)를 활용해 개발되었습니다.

### CLAUDE.md 활용 방식
프로젝트 루트의 `CLAUDE.md`에 아키텍처 규칙, 네이밍 컨벤션, 금지 패턴을 선언형으로 정의했습니다. Claude Code는 매 작업 시 이 파일을 컨텍스트로 참조해 일관된 코드 스타일을 유지합니다. 예를 들어 새 이벤트 타입을 추가할 때 "EventType enum에만 추가" 규칙이 적용되어 흩어진 하드코딩을 방지합니다.

### 아키텍처 결정에 AI 협의 활용
- **AOP vs 직접 호출 방식**: 비즈니스 코드 침투 최소화를 위해 AOP + `@UserEvent` 어노테이션 방식을 채택하는 과정에서 트레이드오프를 AI와 함께 검토했습니다.
- **JSONB vs 별도 테이블**: 이벤트별 가변 속성 저장 전략에서 확장성과 쿼리 성능 간 균형을 논의해 JSONB 컬럼 방식을 선택했습니다.
- **동기 vs 비동기 저장**: 과제의 핵심은 파이프라인 흐름을 명확하게 보여주는 것이라고 판단해, 이벤트 생성 직후 DB에 바로 저장되는 동기 구조를 선택했습니다. 실서비스에서는 큐나 비동기 처리로 확장할 수 있지만, 과제에서는 설명 가능성과 안정성을 우선했습니다.
