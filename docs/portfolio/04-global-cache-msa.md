# ④ 로컬 캐시를 다중 서버로 확장하기 (MSA/CQRS 글로벌 캐시 PoC)

> ②의 인메모리 세그먼트 트리는 서버를 늘리면(Scale-out) 서버마다 캐시가 따로 놀아 정합성이 깨집니다. 이 문제를 재현하려고 Command/Query 분리(CQRS) 환경을 만들었습니다. 그 위에서 L1(Caffeine 트리) + L2(Redis) 캐시와 이벤트 기반 동기화로 **Query 서버 3대가 DB 재조회 없이 같은 값을 유지하는 것**을 검증했습니다.

상세 설계와 다이어그램, 버전별 변경 이력은 PoC 원문 README에 있습니다: [poc/global-cache-msa/README.md](../../poc/global-cache-msa/README.md)

## 구성

| 모듈 | 역할 | 코드 |
|---|---|---|
| api-gateway | 사용자 ID 기반 커스텀 라우팅, Retry, 서버 상태 확인 | [growup-api-gateway](../../poc/global-cache-msa/growup-api-gateway) |
| eureka-server | 서비스 디스커버리 | [growup-eureka-server](../../poc/global-cache-msa/growup-eureka-server) |
| command-api | 쓰기, 변경 이벤트 발행 (Kafka) | [growup-command-api](../../poc/global-cache-msa/growup-command-api) |
| query-api ×3 | 읽기. L1 세그먼트 트리/누적합, L2 Redis, Pub/Sub 무효화, Resilience4j | [growup-query-api](../../poc/global-cache-msa/growup-query-api) |
| legacy-server | 비교 기준. ②의 단일 서버 구현을 옮겨 옴 | [growup-legacy-server](../../poc/global-cache-msa/growup-legacy-server) |
| common | 엔티티, DTO, QueryDSL, MQ 설정 | [growup-common](../../poc/global-cache-msa/growup-common) |
| infra | MySQL Master/Slave, Redis, Kafka(KRaft), RabbitMQ, Toxiproxy | [docker](../../poc/global-cache-msa/docker) |

## 핵심 결정

| 문제 | 결정 |
|---|---|
| Query 서버마다 기동 시 1년치 DB 조회가 중복됨 | L2 Redis에 **트리 데이터** 저장(원문 데이터에서 변경) |
| Broadcast 이벤트는 캐시가 없는 서버가 무시해 L2가 오염될 수 있음 | 같은 Consumer Group + 회원 PK 기반 Partition Key 해싱 |
| L1 무효화 타이밍 | Redis Pub/Sub 시그널 + 지연 이중 삭제(500ms) |
| Redis 장애 | Resilience4j CircuitBreaker + Fallback, Toxiproxy로 장애 주입 |
| 이벤트 중복 처리 | Redis 멱등성 키 |

## 결과 (k6, VU 500, 혼합 부하)

| 환경 | RPS | P95 | Median | 에러율 |
|---|---|---|---|---|
| 단일 서버 (legacy) | 307.69 | 129ms | 7.69ms | 0% |
| MSA 1회차 (cold) | 141 | 2.9s | 1.2s | 0% |
| MSA 2회차 | 265 | 939ms | 55.7ms | 0% |
| MSA 3회차 | 290 | 420ms | 25ms | 0% |

- 근거: [k6 스크립트](../../poc/global-cache-msa/k6Test), [결과 캡처](../../poc/global-cache-msa/k6Test/global-cahce-poc-result), [정합성 검증 결과](../../poc/global-cache-msa/k6Test/global-cahce-poc-result)
- 모든 서비스(JVM 6개, 컨테이너 3개)를 한 대의 머신에서 돌린 측정입니다. 따라서 MSA 처리량이 단일 서버보다 낮은 것은 예상된 결과입니다. 이 PoC의 목적은 **정합성 검증**입니다.

> **재측정 예정**
> - legacy-server에도 ②와 같은 트리 객체 공유 버그([known-issues P1](../known-issues.md))가 있어 비교 기준값이 오염되어 있습니다.
> - query-api의 부분 업데이트 반영 버그는 통합 시점에 수정 커밋으로 반영했습니다.
> - 현재 브랜치에서 RabbitMQ 도입이 진행 중이라 README의 Kafka 설명과 맞춰야 합니다.
