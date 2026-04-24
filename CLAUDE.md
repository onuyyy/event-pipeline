# CLAUDE.md — event-pipeline

## Stack
- Java 21, Spring Boot 3.4.5, Spring Data JPA, PostgreSQL, Lombok, Gradle, Docker Compose, Grafana

## Package Structure
```
com.eventpipeline
├── annotation      # @UserEvent 등 커스텀 어노테이션
├── aspect          # AOP 로깅/이벤트 인터셉터
├── event           # 이벤트 페이로드 클래스 (*Payload.java)
├── domain          # JPA 엔티티
├── repository      # Spring Data JPA 레포지토리
├── service         # 비즈니스 로직
├── controller      # REST 컨트롤러
├── generator       # 이벤트 랜덤 생성기
└── config          # 스프링 설정
```

## Naming
- Class: PascalCase
- Method/Field: camelCase
- 이벤트 페이로드: `*Payload.java`
- AOP 클래스: `*Aspect.java`
- REST 경로: `/api/v1/{resource}`
- DB 테이블/컬럼: `snake_case`

## Code Rules
- 생성자 주입: `@RequiredArgsConstructor` (필드 주입 금지)
- Optional 반환: 반드시 `.orElseThrow()` 명시
- 시간 필드: `LocalDateTime` UTC 기준
- 상수: `enum`으로 관리 — 매직 스트링 금지
- 가변 속성(`properties`): `Map<String, Object>`로 관리하고 JSONB 컬럼에 저장

## Event Pipeline Rules
- 새 이벤트 타입 추가: `EventType` enum 한 곳에만 추가
- AOP 타깃 조건: `public` 메서드 + `@UserEvent` 어노테이션
- `@EventListener` 메서드: 동기 처리 기준으로 구현
- 예외 발생 시: `status = FAILURE`로 이벤트 발행 후 예외 전파

## DB Rules
- PK: `BIGSERIAL` (auto-increment)
- 고정 필드(event_type, user_id, status 등): 전용 컬럼으로 분리
- 가변 속성만: `properties JSONB`에 저장
- 인덱스 필수: `event_type`, `user_id`, `session_id`, `event_time`

## Test Rules
- 단위 테스트: Mockito, 메서드명 `given_when_then` 패턴
- JPA 테스트: `@DataJpaTest` + H2 인메모리
- 통합 테스트 금지 (Docker 의존성)
- Assertion: AssertJ (`assertThat`) 사용

## Query Rules
- 집계 쿼리: `@Query(nativeQuery = true)` 사용
- N+1 방지: fetch join 또는 `@EntityGraph` 적용

## Skip Rules
응답 작성 시 아래 항목 생략 가능:
- `import` 문
- getter/setter (Lombok `@Data`/`@Getter`/`@Setter` 사용)
- 자명한 동작 주석
